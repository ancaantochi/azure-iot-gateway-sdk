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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.core.IGatewayModule;

/**
 * A Proxy for the remote module that can attach to the Azure IoT Gateway to
 * receive and send messages. The proxy handles creating a module instance and
 * calls create and start methods from the module. The module that is
 * instantiated by the proxy is specified in the configuration. It handles the
 * communication to and from the Gateway and forwards the messages to the
 * module.
 * 
 * <p>
 * The {@code attach} method is creating a communication channel with the
 * Gateway and starts listening for messages from the Gateway. Once the
 * initialization messages are received from the Gateway it creates an instance
 * of the IGatewayModule, calls create and start on the module and forwards
 * received messages from the Gateway. If it receives a destroy message from the
 * Gateway it calls destroy on the module.
 * 
 * <p>
 * The {@code detach} method stops receiving new messages and sends a notify
 * message to the Gateway.
 * 
 * <h3>Usage Examples</h3>
 *
 * Here is how an existing Printer, that implements
 * {@link com.microsoft.azure.gateway.core.IGatewayModule IGatewayModule}
 * module, can be run used as a remote module:
 *
 * <pre>
 *  {@code
 *  String id = "control-id";
 *  byte version = 1;
 *  ModuleConfiguration.Builder configBuilder = new ModuleConfiguration.Builder();
 *  configBuilder.setIdentifier(id);
 *  configBuilder.setModuleClass(Printer.class);
 *  configBuilder.setModuleVersion(version);
 *      
 *  RemoteModuleProxy moduleProxy = new RemoteModuleProxy(configBuilder.build());
 *  try {
 *       moduleProxy.attach();
 *  } catch (ConnectionException e) {
 *       e.printStackTrace();
 *  }
 * }}
 * </pre>
 *
 */
public class ProxyGateway {

    private static final int DEFAULT_DELAY_MILLIS = 10;
    private final ModuleConfiguration config;
    private final Object lock = new Object();
    private boolean isAttached;
    private ScheduledExecutorService executor;
    private MessageListener receiveMessageListener;

    public ProxyGateway(ModuleConfiguration configuration) {
        if (configuration == null)
            throw new IllegalArgumentException("Configuration can not be null.");

        this.config = configuration;
    }

    /**
     * Attach the remote module to the Gateway. It creates the communication
     * channel with the Gateway and starts a new thread that is listening for
     * messages.
     *
     * @throws ConnectionException
     *             If it can not connect to the Gateway communication channel
     */
    public void attach() throws ConnectionException {
        synchronized (lock) {
            if (!this.isAttached) {
                this.isAttached = true;
                this.receiveMessageListener = new MessageListener(config);
                this.executor = Executors.newSingleThreadScheduledExecutor();
                this.startListening();
            }
        }
    }

    /**
     * Detach from the Gateway. The listening of messages from the Gateway is
     * terminated and destroy method on IGatewayModule instance is called. A
     * notification message is sent to the Gateway
     */
    public void detach() {
        boolean sendDetachToGateway = true;
        this.detach(sendDetachToGateway);
    }

    void startListening() {
        this.executor.scheduleWithFixedDelay(receiveMessageListener, 0, DEFAULT_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    }

    boolean isAttached() {
        return this.isAttached;
    }

    MessageListener getReceiveMessageListener() {
        return this.receiveMessageListener;
    }

    ScheduledExecutorService getExecutor() {
        return this.executor;
    }

    private void detach(boolean sendDetachToGateway) {
        synchronized (lock) {
            if (this.isAttached) {
                this.executor.shutdownNow();
                try {
                    executor.awaitTermination(60, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                }

                this.receiveMessageListener.detach(true);
                this.isAttached = false;
            }
        }
    }

    class MessageListener implements Runnable {
        private final ModuleConfiguration config;
        private final Logger logger = LoggerFactory.getLogger(ProxyGateway.class);
        private CommunicationEndpoint controlEndpoint;
        private CommunicationEndpoint dataEndpoint;
        private IGatewayModule module;

        public MessageListener(ModuleConfiguration config) throws ConnectionException {
            this.config = config;
            this.controlEndpoint = new CommunicationEndpoint(this.config.getIdentifier(),
                    new CommunicationControlStrategy());
            this.controlEndpoint.setVersion(this.config.getVersion());
            this.controlEndpoint.connect();
        }

        @Override
        public void run() {
            this.executeControlMessage();
            this.executeDataMessage();
        }

        public void detach(boolean sendDetachToGateway) {
            if (this.module != null)
                this.module.destroy();

            if (sendDetachToGateway) {
                this.sendControlReplyMessage(RemoteModuleReplyCode.DETACH.getValue());

                try {
                    // sleep for a second so the Gateway has time to receive the
                    // message before closing the connection
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                this.controlEndpoint.disconnect();
            }

            if (this.dataEndpoint != null)
                this.dataEndpoint.disconnect();

            this.module = null;
        }

        void executeControlMessage() {
            RemoteMessage message = null;
            try {
                message = this.controlEndpoint.receiveMessage();
            } catch (MessageDeserializationException e) {
                logger.error(e.toString());
                this.sendControlReplyMessage(RemoteModuleReplyCode.CONNECTION_ERROR.getValue());
                this.disconnectDataMessage();
            } catch (ConnectionException e) {
                logger.error(e.toString());
                this.sendControlReplyMessage(RemoteModuleReplyCode.CONNECTION_ERROR.getValue());
                this.disconnectDataMessage();
            }
            if (message != null) {
                ControlMessage controlMessage = (ControlMessage) message;
                if (controlMessage.getMessageType() == RemoteMessageType.CREATE) {
                    try {
                        this.processCreateMessage(message, this.controlEndpoint);
                        boolean sent = sendControlReplyMessage(RemoteModuleReplyCode.OK.getValue());
                        if (!sent)
                            this.disconnectDataMessage();
                    } catch (ConnectionException e) {
                        logger.error(e.toString());
                        this.sendControlReplyMessage(RemoteModuleReplyCode.CONNECTION_ERROR.getValue());
                        this.disconnectDataMessage();
                    } catch (ModuleInstantiationException e) {
                        logger.error(e.toString());
                        this.sendControlReplyMessage(RemoteModuleReplyCode.CREATION_ERROR.getValue());
                        this.disconnectDataMessage();
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

        void executeDataMessage() {
            try {
                if (this.dataEndpoint != null) {
                    RemoteMessage dataMessage = this.dataEndpoint.receiveMessage();
                    if (dataMessage != null) {
                        this.module.receive(((DataMessage) dataMessage).getContent());
                    }
                }
            } catch (ConnectionException e) {
                logger.error(e.toString());
            } catch (MessageDeserializationException e) {
                logger.error(e.toString());
            }
        }

        private void processDestroyMessage() {
            this.detach(false);
        }

        private void processStartMessage() {
            if (this.module == null)
                return;

            this.module.start();
        }

        private void processCreateMessage(RemoteMessage message, CommunicationEndpoint endpoint)
                throws ConnectionException, ModuleInstantiationException {
            this.disconnectDataMessage();

            CreateMessage controlMessage = (CreateMessage) message;
            this.dataEndpoint = this.createDataEndpoints(controlMessage.getDataEndpoint());

            try {
                this.createModuleInstanceWithArgsConstructor(controlMessage, this.dataEndpoint);

                if (this.module == null) {
                    this.createModuleInstanceNoArgsConstructor(controlMessage, this.dataEndpoint);
                }
            } catch (InstantiationException e) {
                logger.error(e.toString());
                throw new ModuleInstantiationException("Could not instantiate module", e);
            } catch (IllegalAccessException e) {
                logger.error(e.toString());
                throw new ModuleInstantiationException("Could not instantiate module", e);
            } catch (IllegalArgumentException e) {
                logger.error(e.toString());
                throw new ModuleInstantiationException("Could not instantiate module", e);
            } catch (InvocationTargetException e) {
                logger.error(e.toString());
                throw new ModuleInstantiationException("Could not instantiate module", e);
            }
        }

        private void disconnectDataMessage() {
            if (this.module != null) {
                this.module.destroy();
                this.module = null;
            }
            if (this.dataEndpoint != null)
                this.dataEndpoint.disconnect();
        }

        private boolean sendControlReplyMessage(int code) {
            byte[] createCompletedMessage = new MessageSerializer().serializeMessage(code,
                    this.controlEndpoint.getVersion());
            boolean sent = false;

            try {
                sent = this.controlEndpoint.sendMessageAsync(createCompletedMessage);
            } catch (ConnectionException e) {
                logger.error(e.toString());
            }
            return sent;
        }

        private void createModuleInstanceNoArgsConstructor(CreateMessage controlMessage,
                CommunicationEndpoint dataEndpoint) throws InstantiationException, IllegalAccessException {
            final int emptyAddress = 0;
            this.module = this.config.getModuleClass().newInstance();
            this.module.create(emptyAddress, new BrokerProxy(dataEndpoint), controlMessage.getArgs());
        }

        private void createModuleInstanceWithArgsConstructor(CreateMessage controlMessage,
                CommunicationEndpoint dataEndpoint) throws InstantiationException, IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            Class<? extends IGatewayModule> clazz = this.config.getModuleClass();
            final int emptyAddress = 0;
            try {
                Constructor<? extends IGatewayModule> ctor = clazz.getDeclaredConstructor(long.class, Broker.class,
                        String.class);
                this.module = ctor.newInstance(emptyAddress, new BrokerProxy(dataEndpoint), controlMessage.getArgs());
            } catch (NoSuchMethodException e) {
                logger.error(e.toString());
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