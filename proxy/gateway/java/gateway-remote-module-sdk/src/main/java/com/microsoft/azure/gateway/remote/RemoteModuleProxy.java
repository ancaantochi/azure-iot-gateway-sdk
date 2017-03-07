/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.core.IGatewayModule;

public class RemoteModuleProxy {

    private static final int DEFAULT_DELAY_MILIS = 200;
    private final ModuleConfiguration config;
    private boolean isAttached;
    private CommunicationEndpoint controlEndpoint;
    private CommunicationEndpoint dataEndpoint;
    private IGatewayModule module;
    private int messageVersion;
    private ScheduledExecutorService executor;

    public RemoteModuleProxy(ModuleConfiguration configuration) {
        if (configuration == null)
            throw new IllegalArgumentException("Configuration can not be null.");

        this.config = configuration;
    }

    public void attach() throws ConnectionException {
        if (!isAttached) {
            synchronized (config) {
                if (!isAttached) {
                    controlEndpoint = new CommunicationEndpoint(this.config.getIdentifier(),
                            new CommunicationControlStrategy());
                    controlEndpoint.connect();
                    isAttached = true;
                    this.startListening();
                }
            }
        }
    }

    public void detach() {
        boolean sendDetachToGateway = true;
        detach(sendDetachToGateway);
    }

    private void detach(boolean sendDetachToGateway) {
        if (isAttached) {
            synchronized (config) {
                if (isAttached) {
                    executor.shutdownNow();

                    if (module != null)
                        module.destroy();

                    if (sendDetachToGateway) {
                        byte[] detachMessage = new MessageSerializer()
                                .serializeMessage(RemoteModuleResultCode.DETACH.getValue(), this.messageVersion);
                        try {
                            this.controlEndpoint.sendMessageAsync(detachMessage);
                        } catch (ConnectionException e) {
                            e.printStackTrace();
                        }
                    }

                    this.controlEndpoint.disconnect();
                    if (this.dataEndpoint != null)
                        this.dataEndpoint.disconnect();

                    isAttached = false;
                    executor = null;
                    module = null;
                }
            }
        }
    }

    void startListening() {
        if (!isAttached)
            throw new IllegalStateException("Please call attach before starting listening.");
        if (executor != null)
            throw new IllegalStateException("Thread already listening");

        Runnable receiveMessage = new MessageListener();
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(receiveMessage, 0, DEFAULT_DELAY_MILIS, TimeUnit.MILLISECONDS);
    }

    private class MessageListener implements Runnable {

        @Override
        public void run() {
            executeControlMessage();
            executeDataMessage();
        }

        private void executeDataMessage() {
            try {
                if (dataEndpoint != null) {
                    RemoteMessage dataMessage = dataEndpoint.receiveMessage();
                    if (dataMessage != null) {
                        module.receive(((DataMessage) dataMessage).getContent());
                    }
                }
            } catch (ConnectionException e) {
                e.printStackTrace();

            } catch (MessageDeserializationException e) {
                e.printStackTrace();
            }
        }

        private void executeControlMessage() {
            RemoteMessage message = null;
            try {
                message = controlEndpoint.receiveMessage();
            } catch (MessageDeserializationException e) {
                e.printStackTrace();
                sendControlReplyMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue());
                detach(false);
            } catch (ConnectionException e) {
                e.printStackTrace();
                sendControlReplyMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue());
                detach(false);
            }
            if (message != null) {
                if (message instanceof CreateMessage) {
                    try {
                        this.processCreateMessage(message, controlEndpoint);
                    } catch (ConnectionException e) {
                        e.printStackTrace();
                        sendControlReplyMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue());
                        detach(false);
                    } catch (ModuleInstantiationException e) {
                        e.printStackTrace();
                        sendControlReplyMessage(RemoteModuleResultCode.CREATION_ERROR.getValue());
                        detach(false);
                    }

                    boolean sent = sendControlReplyMessage(RemoteModuleResultCode.OK.getValue());
                    if (!sent)
                        detach(false);

                    return;
                }

                if (message instanceof StartMessage) {
                    this.processStartMessage();
                    return;
                }

                if (message instanceof DestroyMessage) {
                    this.processDestroyMessage();
                    return;
                }
            }
        }

        private void processDestroyMessage() {
            if (module == null)
                throw new IllegalStateException("Module has to be initialized before calling destroy.");

            detach(false);
        }

        private void processStartMessage() {
            if (module == null)
                throw new IllegalStateException("Module has to be initialized before calling start.");

            module.start();
        }

        private void processCreateMessage(RemoteMessage message, CommunicationEndpoint endpoint)
                throws ConnectionException, ModuleInstantiationException {
            if (module != null) {
                module.destroy();
                module = null;
                dataEndpoint.disconnect();
            }

            CreateMessage controlMessage = (CreateMessage) message;
            messageVersion = controlMessage.getVersion();
            dataEndpoint = this.createDataEndpoints(controlMessage.getDataEndpoint());

            try {
                this.createModuleInstanceWithArgsConstructor(controlMessage, dataEndpoint);

                if (module == null) {
                    this.createModuleInstanceNoArgsConstructor(controlMessage, dataEndpoint);
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
                throw new ModuleInstantiationException("Could not instantiate module", e);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new ModuleInstantiationException("ould not instantiate module", e);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                throw new ModuleInstantiationException("Could not instantiate module", e);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new ConnectionException("ould not instantiate module", e);
            }
        }

        private boolean sendControlReplyMessage(int code) {
            byte[] createCompletedMessage = new MessageSerializer().serializeMessage(code, messageVersion);
            boolean sent = false;

            try {
                sent = controlEndpoint.sendMessageAsync(createCompletedMessage);
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
            return sent;
        }

        private void createModuleInstanceNoArgsConstructor(CreateMessage controlMessage,
                CommunicationEndpoint dataEndpoint) throws InstantiationException, IllegalAccessException {
            final int emptyAddress = 0;
            module = config.getModuleClass().newInstance();
            module.create(emptyAddress, new BrokerProxy(dataEndpoint), controlMessage.getArgs());
        }

        private void createModuleInstanceWithArgsConstructor(CreateMessage controlMessage,
                CommunicationEndpoint dataEndpoint) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            Class<? extends IGatewayModule> clazz = config.getModuleClass();
            final int emptyAddress = 0;
            try {
                Constructor<? extends IGatewayModule> ctor = clazz.getDeclaredConstructor(long.class, Broker.class,
                        String.class);
                module = ctor.newInstance(emptyAddress, new BrokerProxy(dataEndpoint), controlMessage.getArgs());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        private CommunicationEndpoint createDataEndpoints(DataEndpointConfig endpointConfig)
                throws ConnectionException {
            CommunicationEndpoint endpoint = new CommunicationEndpoint(endpointConfig.getId(),
                    new CommunicationDataStrategy(endpointConfig.getType()));
            endpoint.connect();
            return endpoint;
        }
    }

}