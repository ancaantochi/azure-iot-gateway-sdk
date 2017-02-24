package com.microsoft.azure.gateway.remote;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.core.IGatewayModule;

public class RemoteModuleProxy {

    private static final int DEFAULT_DELAY_MILIS = 200;
    private final ModuleConfiguration config;
    private boolean isAttached;
    private List<CommunicationEndpoint> endpoints;
    private IGatewayModule module;
    private ScheduledFuture<?> future;

    public RemoteModuleProxy(ModuleConfiguration configuration) {
        if (configuration == null)
            throw new IllegalArgumentException("Configuration can not be null.");

        this.config = configuration;
        this.endpoints = new ArrayList<CommunicationEndpoint>();
    }

    public void attach() throws ConnectionException {
        if (isAttached)
            return;

        CommunicationEndpoint controlEndpoint = new CommunicationEndpoint(this.config.getIdentifier(),
                new CommunicationControlStrategy());
        controlEndpoint.connect();
        endpoints.add(controlEndpoint);
        isAttached = true;
        this.startListening();
    }

    public void detach() {
        if (!isAttached)
            return;

        future.cancel(true);
        for (CommunicationEndpoint endpoint : endpoints) {
            endpoint.disconnect();
        }

        isAttached = false;
        future = null;
        module = null;
    }

    private void startListening() {
        if (!isAttached)
            throw new IllegalStateException("Please call attach before starting listening.");
        if (future != null)
            throw new IllegalStateException("Thread already listening");

        Runnable receiveMessage = new MessageListener();
        future = Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(receiveMessage, 0,
                DEFAULT_DELAY_MILIS, TimeUnit.MILLISECONDS);
    }

    private class MessageListener implements Runnable {

        @Override
        public void run() {
            try {

                for (CommunicationEndpoint endpoint : endpoints) {
                    RemoteMessage message = endpoint.receiveMessage();
                    if (message != null) {
                        if (message instanceof DataMessage) {
                            executeDataMessage((DataMessage) message);
                        } else {
                            executeControlMessage(message, endpoint);
                        }
                    }
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
            } catch (MessageDeserializationException e) {
                e.printStackTrace();
            }
        }

        private void executeDataMessage(DataMessage dataMessage) {
            module.receive(dataMessage.getContent());
        }

        private void executeControlMessage(RemoteMessage message, CommunicationEndpoint endpoint)
                throws ConnectionException {
            if (message instanceof CreateMessage) {
                this.processCreateMessage(message, endpoint);
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

        private void processDestroyMessage() {
            if (module == null)
                throw new IllegalStateException("Module has to be initialized before calling destroy.");

            detach();
            module.destroy();
        }

        private void processStartMessage() {
            if (module == null)
                throw new IllegalStateException("Module has to be initialized before calling start.");

            module.start();
        }

        private void processCreateMessage(RemoteMessage message, CommunicationEndpoint endpoint)
                throws ConnectionException {
            CreateMessage controlMessage = (CreateMessage) message;
            CommunicationEndpoint dataEndpoint = this.createDataEndpoints(controlMessage.getDataEndpoints());
            endpoints.add(dataEndpoint);

            this.createModuleInstanceWithArgsConstructor(controlMessage, dataEndpoint);

            if (module == null) {
                this.createModuleInstanceNoArgsConstructor(controlMessage, dataEndpoint);
            }

            byte[] createCompletedMessage = new MessageSerializer().serializeCreateCompleted(true,
                    controlMessage.getVersion());
            endpoint.sendMessage(createCompletedMessage);
        }

        private void createModuleInstanceNoArgsConstructor(CreateMessage controlMessage,
                CommunicationEndpoint dataEndpoint) {
            try {
                final int emptyAddress = 0;
                module = config.getModuleClass().newInstance();
                module.create(emptyAddress, new BrokerProxy(dataEndpoint), controlMessage.getArgs());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private void createModuleInstanceWithArgsConstructor(CreateMessage controlMessage,
                CommunicationEndpoint dataEndpoint) {
            Class<? extends IGatewayModule> clazz = config.getModuleClass();
            final int emptyAddress = 0;
            try {
                Constructor<? extends IGatewayModule> ctor = clazz.getDeclaredConstructor(Long.class, Broker.class,
                        String.class);
                module = ctor.newInstance(emptyAddress, new BrokerProxy(dataEndpoint), controlMessage.getArgs());
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
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