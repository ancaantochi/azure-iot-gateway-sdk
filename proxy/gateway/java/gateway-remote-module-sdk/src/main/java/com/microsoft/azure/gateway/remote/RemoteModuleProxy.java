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
    private Runnable receiveMessageListener;

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
                    receiveMessageListener = new MessageListener();
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
                    receiveMessageListener = null;
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

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(receiveMessageListener, 0, DEFAULT_DELAY_MILIS, TimeUnit.MILLISECONDS);
    }

    private CommunicationEndpoint getDataEndpoint() {
        return dataEndpoint;
    }

    private void setDataEndpoint(CommunicationEndpoint value) {
        dataEndpoint = value;
    }

    private IGatewayModule getModule() {
        return module;
    }

    private void setModule(IGatewayModule value) {
        module = value;
    }

    private CommunicationEndpoint getControlEndpoint() {
        return controlEndpoint;
    }

    private void setMessageVersion(int version) {
        messageVersion = version;
    }

    private ModuleConfiguration getConfig() {
        return config;
    }

    private int getMessageVersion() {
        return messageVersion;
    }

    class MessageListener implements Runnable {

        @Override
        public void run() {
            executeControlMessage();
            executeDataMessage();
        }

        void executeDataMessage() {
            try {
                if (getDataEndpoint() != null) {
                    RemoteMessage dataMessage = getDataEndpoint().receiveMessage();
                    if (dataMessage != null) {
                        getModule().receive(((DataMessage) dataMessage).getContent());
                    }
                }
            } catch (ConnectionException e) {
                e.printStackTrace();

            } catch (MessageDeserializationException e) {
                e.printStackTrace();
            }
        }

        void executeControlMessage() {
            RemoteMessage message = null;
            try {
                message = getControlEndpoint().receiveMessage();
            } catch (MessageDeserializationException e) {
                e.printStackTrace();
                sendControlReplyMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue());
                disconnectDataMessage();
            } catch (ConnectionException e) {
                e.printStackTrace();
                sendControlReplyMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue());
                disconnectDataMessage();
            }
            if (message != null) {
                ControlMessage controlMessage = (ControlMessage)message;
                if (controlMessage.getMessageType() == RemoteMessageType.CREATE) {
                    try {
                        this.processCreateMessage(message, getControlEndpoint());
                        boolean sent = sendControlReplyMessage(RemoteModuleResultCode.OK.getValue());
                        if (!sent)
                            disconnectDataMessage();
                    } catch (ConnectionException e) {
                        e.printStackTrace();
                        sendControlReplyMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue());
                        disconnectDataMessage();
                    } catch (ModuleInstantiationException e) {
                        e.printStackTrace();
                        sendControlReplyMessage(RemoteModuleResultCode.CREATION_ERROR.getValue());
                        disconnectDataMessage();
                    }
                }

                if (controlMessage.getMessageType() == RemoteMessageType.START) {
                    this.processStartMessage();
                }

                if (controlMessage.getMessageType() == RemoteMessageType.DESTROY) {
                    this.processDestroyMessage();
                }
            }
        }

        private void processDestroyMessage() {
            detach(false);
        }

        private void processStartMessage() {
            if (getModule() == null)
                return;

            getModule().start();
        }

        private void processCreateMessage(RemoteMessage message, CommunicationEndpoint endpoint)
                throws ConnectionException, ModuleInstantiationException {
            disconnectDataMessage();

            CreateMessage controlMessage = (CreateMessage) message;
            setMessageVersion(controlMessage.getVersion());
            setDataEndpoint(this.createDataEndpoints(controlMessage.getDataEndpoint()));

            try {
                this.createModuleInstanceWithArgsConstructor(controlMessage, getDataEndpoint());

                if (getModule() == null) {
                    this.createModuleInstanceNoArgsConstructor(controlMessage, getDataEndpoint());
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
                throw new ModuleInstantiationException("Could not instantiate module", e);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new ModuleInstantiationException("Could not instantiate module", e);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                throw new ModuleInstantiationException("Could not instantiate module", e);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new ModuleInstantiationException("Could not instantiate module", e);
            }
        }

        private void disconnectDataMessage() {
            if (getModule() != null) {
                getModule().destroy();
                setModule(null);
            }
            if (getDataEndpoint() != null)
                getDataEndpoint().disconnect();
        }

        private boolean sendControlReplyMessage(int code) {
            byte[] createCompletedMessage = new MessageSerializer().serializeMessage(code, getMessageVersion());
            boolean sent = false;

            try {
                sent = getControlEndpoint().sendMessageAsync(createCompletedMessage);
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
            return sent;
        }

        private void createModuleInstanceNoArgsConstructor(CreateMessage controlMessage,
                CommunicationEndpoint dataEndpoint) throws InstantiationException, IllegalAccessException {
            final int emptyAddress = 0;
            setModule(getConfig().getModuleClass().newInstance());
            getModule().create(emptyAddress, new BrokerProxy(dataEndpoint), controlMessage.getArgs());
        }

        private void createModuleInstanceWithArgsConstructor(CreateMessage controlMessage,
                CommunicationEndpoint dataEndpoint) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            Class<? extends IGatewayModule> clazz = getConfig().getModuleClass();
            final int emptyAddress = 0;
            try {
                Constructor<? extends IGatewayModule> ctor = clazz.getDeclaredConstructor(long.class, Broker.class,
                        String.class);
                setModule(ctor.newInstance(emptyAddress, new BrokerProxy(dataEndpoint), controlMessage.getArgs()));
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