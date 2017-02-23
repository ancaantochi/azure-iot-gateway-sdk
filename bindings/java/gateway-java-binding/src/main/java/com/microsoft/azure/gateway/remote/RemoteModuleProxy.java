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

        CommunicationEndpoint controlEndpoint = new CommunicationControlEndpoint(this.config.getIdentifier());
        controlEndpoint.connect();
        endpoints.add(controlEndpoint);
        isAttached = true;
        this.startListening();
    }

    public void detach() throws ConnectionException {
        if (!isAttached)
            return;

        future.cancel(true);
        for (CommunicationEndpoint endpoint : endpoints) {
            endpoint.disconnect();
        }

        isAttached = false;
        future = null;
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
                        if (endpoint instanceof CommunicationControlEndpoint) {
                            executeControlMessage(message, (CommunicationControlEndpoint) endpoint);
                        } else {
                            executeDataMessage((DataMessage) message);
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

        private void executeControlMessage(RemoteMessage message, CommunicationControlEndpoint endpoint)
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

            try {
                detach();
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
            module.destroy();
        }

        private void processStartMessage() {
            if (module == null)
                throw new IllegalStateException("Module has to be initialized before calling start.");

            module.start();
        }

        private void processCreateMessage(RemoteMessage message, CommunicationControlEndpoint endpoint)
                throws ConnectionException {
            CreateMessage controlMessage = (CreateMessage) message;
            this.createModuleInstanceWithArgsConstructor(controlMessage);

            if (module == null) {
                this.createModuleInstanceNoArgsConstructor(controlMessage);
            }

            this.createDataEndpoints(controlMessage.getDataEndpoints());
            endpoint.sendCreateReply(controlMessage.getVersion());
        }

        private void createModuleInstanceNoArgsConstructor(CreateMessage controlMessage) {
            try {
                final int emptyAddress = 0;
                module = config.getModuleClass().newInstance();
                module.create(emptyAddress, new BrokerProxy(), controlMessage.getArgs());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private void createModuleInstanceWithArgsConstructor(CreateMessage controlMessage) {
            Class<? extends IGatewayModule> clazz = config.getModuleClass();
            final int emptyAddress = 0;
            try {
                Constructor<? extends IGatewayModule> ctor = clazz.getDeclaredConstructor(Long.class, Broker.class,
                        String.class);
                module = ctor.newInstance(emptyAddress, new BrokerProxy(), controlMessage.getArgs());
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

        private void createDataEndpoints(List<DataEndpointConfig> dataEndpoints) throws ConnectionException {
            for (DataEndpointConfig endpointConfig : dataEndpoints) {
                CommunicationEndpoint endpoint = new CommunicationDataEndpoint(endpointConfig.getId(),
                        endpointConfig.getType());
                endpoint.connect();
                endpoints.add(endpoint);
            }
        }
    }

}