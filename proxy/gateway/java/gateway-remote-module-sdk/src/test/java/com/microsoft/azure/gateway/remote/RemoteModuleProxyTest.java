/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nanomsg.NanoLibrary;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.core.IGatewayModule;
import com.microsoft.azure.gateway.remote.RemoteModuleProxy.MessageListener;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;

public class RemoteModuleProxyTest {

    private final static String identifier = "test";
    private String dataSocketId = "data-test";
    private String args = "";
    private static int version = 1;
    private static byte[] messageBuffer = new byte[10];
    private static ModuleConfiguration config;

    @BeforeClass
    public static void applySharedMockups() {
        ModuleConfiguration.Builder configBuilder = new ModuleConfiguration.Builder();
        configBuilder.setIdentifier(identifier);
        configBuilder.setModuleClass(TestModuleImplementsInterface.class);
        configBuilder.setModuleVersion(version);

        config = configBuilder.build();

        new MockUp<NanoLibrary>() {
            @Mock
            int nn_socket(int i, int j) {
                return 0;
            }

            @Mock
            public int nn_errno() {
                return 1;
            }

            @Mock
            public String nn_strerror(int errnum) {
                return "Error " + errnum;
            }

            @Mock
            int nn_bind(int socket, String uri) {
                return 1;
            }

            @Mock
            void ensureNativeCode() {
            }

            @Mock
            int load_symbols(Map<String, Integer> map) {
                map.put("NN_PAIR", 16);
                return 0;
            }

            @Mock
            int get_symbol(String name) {
                return 1;
            }

            @Mock
            public int nn_shutdown(int socket, int how) {
                return 0;
            }

            @Mock
            public int nn_close(int socket) {
                return 0;
            }

            @Mock
            public byte[] nn_recvbyte(int socket, int flags) {
                return messageBuffer;
            }

            @Mock
            public int nn_sendbyte(int socket, byte[] str, int flags) {
                return 0;
            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldThrowWhenInvalidArguments() {
        new RemoteModuleProxy(null);
    }

    @Test
    public void attachSuccessWhenAlreadyAttached(@Mocked final CommunicationEndpoint controlEndpoint)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };

        proxy.attach();
        proxy.attach();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
            }
        };
    }

    @Test(expected = ConnectionException.class)
    public void attachShouldThrowWhenConnectFails(@Mocked final CommunicationEndpoint controlEndpoint)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);

        new Expectations() {
            {
                controlEndpoint.connect();
                result = new ConnectionException();
            }
        };

        proxy.attach();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
            }
        };
    }

    @Test
    public void attachShouldCallStartListening(@Mocked final CommunicationEndpoint controlEndpoint)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };

        proxy.attach();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
                proxy.startListening();
                times = 1;
            }
        };
    }

    @Test
    public void attachShouldCreateModuleInstanceNoArgsConstructor(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint, @Mocked final TestModuleImplementsInterface module)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final RemoteMessage nullMessage = null;

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;

                new TestModuleImplementsInterface();
                result = module;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                returns(createMessage);
                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;

                dataEndpoint.connect();
                dataEndpoint.receiveMessage();
                returns(nullMessage);
            }
        };

        proxy.attach();
        Runnable receiveMessage = Deencapsulation.getField(proxy, "receiveMessageListener");
        receiveMessage.run();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;
                dataEndpoint.connect();
                times = 1;

                module.create(anyLong, (Broker) any, anyString);
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldCreateModuleInstanceArgsConstructor(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint)
            throws ConnectionException, MessageDeserializationException {

        ModuleConfiguration.Builder configBuilder = new ModuleConfiguration.Builder();
        configBuilder.setIdentifier(identifier);
        configBuilder.setModuleClass(TestModuleExtendsAbstractClass.class);
        configBuilder.setModuleVersion(1);

        final RemoteModuleProxy proxy = new RemoteModuleProxy(configBuilder.build());
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final RemoteMessage nullMessage = null;

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                returns(createMessage);
                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;

                dataEndpoint.connect();
                dataEndpoint.receiveMessage();
                returns(nullMessage);
            }
        };

        proxy.attach();
        Runnable receiveMessage = Deencapsulation.getField(proxy, "receiveMessageListener");
        receiveMessage.run();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;
                dataEndpoint.connect();
                times = 1;
            }
        };

//        IGatewayModule module = Deencapsulation.getField(proxy, "module");
        assertTrue(proxy.isAttached());
//        assertNotNull(module);
    }

    @Test
    public void attachShouldHandleCreateMessageWhenAlreadyCreated(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint, @Mocked final TestModuleImplementsInterface module,
            @Mocked final MessageSerializer serializer) throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final RemoteMessage nullMessage = null;

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;

                new TestModuleImplementsInterface();
                result = module;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                returns(createMessage, createMessage);
                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;

                serializer.serializeMessage(RemoteModuleResultCode.OK.getValue(), version);

                dataEndpoint.connect();
                dataEndpoint.receiveMessage();
                returns(nullMessage);
            }
        };

        proxy.attach();
        RemoteModuleProxy.MessageListener receiveMessage = Deencapsulation.getField(proxy, "receiveMessageListener");
        receiveMessage.executeControlMessage();
        receiveMessage.executeDataMessage();

        receiveMessage.executeControlMessage();
        receiveMessage.executeDataMessage();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 2;

                dataEndpoint.connect();
                times = 2;
                dataEndpoint.disconnect();
                times = 1;

                module.create(anyLong, (Broker) any, anyString);
                times = 2;
                module.destroy();
                times = 1;

                serializer.serializeMessage(RemoteModuleResultCode.OK.getValue(), version);
                times = 2;
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldNotInstantiateModuleWhenMessageDeserializationException(
            @Mocked final CommunicationEndpoint controlEndpoint, @Mocked final TestModuleImplementsInterface module,
            @Mocked final MessageSerializer serializer) throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                result = new MessageDeserializationException();

                serializer.serializeMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue(), 0);

                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;
            }
        };

        proxy.attach();
        RemoteModuleProxy.MessageListener receiveMessage = Deencapsulation.getField(proxy, "receiveMessageListener");
        receiveMessage.executeControlMessage();
        receiveMessage.executeDataMessage();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;

                serializer.serializeMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue(), 0);
                times = 1;

                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;

                new TestModuleImplementsInterface();
                times = 0;
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldNotInstantiateModuleWhenConnectionException(
            @Mocked final CommunicationEndpoint controlEndpoint, @Mocked final TestModuleImplementsInterface module,
            @Mocked final MessageSerializer serializer) throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                result = new ConnectionException();

                serializer.serializeMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue(), 0);

                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;
            }
        };

        proxy.attach();
        RemoteModuleProxy.MessageListener receiveMessage = Deencapsulation.getField(proxy, "receiveMessageListener");
        receiveMessage.executeControlMessage();
        receiveMessage.executeDataMessage();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;

                serializer.serializeMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue(), 0);
                times = 1;

                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;

                new TestModuleImplementsInterface();
                times = 0;
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldHandleCreateDataEndpointFail(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint, @Mocked final MessageSerializer serializer)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                result = createMessage;

                dataEndpoint.connect();
                result = new ConnectionException();

                serializer.serializeMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue(), version);

                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;
            }
        };

        proxy.attach();
        RemoteModuleProxy.MessageListener receiveMessage = Deencapsulation.getField(proxy, "receiveMessageListener");
        receiveMessage.executeControlMessage();
        receiveMessage.executeDataMessage();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;

                serializer.serializeMessage(RemoteModuleResultCode.CONNECTION_ERROR.getValue(), version);
                times = 1;

                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldSendCreationErrorWhenModuleInstantionFails(
            @Mocked final CommunicationEndpoint controlEndpoint, @Mocked final CommunicationEndpoint dataEndpoint,
            @Mocked final TestModuleImplementsInterface module, @Mocked final MessageSerializer serializer)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                result = createMessage;

                dataEndpoint.receiveMessage();
                result = null;

                serializer.serializeMessage(RemoteModuleResultCode.CREATION_ERROR.getValue(), version);

                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;

                new TestModuleImplementsInterface();
                result = new InstantiationException();
            }
        };

        proxy.attach();
        RemoteModuleProxy.MessageListener receiveMessage = Deencapsulation.getField(proxy, "receiveMessageListener");
        receiveMessage.executeControlMessage();
        receiveMessage.executeDataMessage();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;

                serializer.serializeMessage(RemoteModuleResultCode.CREATION_ERROR.getValue(), version);
                times = 1;

                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;

                new TestModuleImplementsInterface();
                times = 1;
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldCallModuleStart(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint, @Mocked final TestModuleImplementsInterface module)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final ControlMessage startMessage = new ControlMessage(RemoteMessageType.START);
        final RemoteMessage nullMessage = null;

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;

                new TestModuleImplementsInterface();
                result = module;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                returns(createMessage, startMessage);
                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;

                dataEndpoint.connect();
                dataEndpoint.receiveMessage();
                returns(nullMessage);
            }
        };

        proxy.attach();
        MessageListener messageListener = Deencapsulation.getField(proxy, "receiveMessageListener");
        messageListener.executeControlMessage();
        messageListener.executeDataMessage();

        messageListener.executeControlMessage();
        messageListener.executeDataMessage();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;
                dataEndpoint.connect();
                times = 1;

                module.create(anyLong, (Broker) any, anyString);
                times = 1;
                module.start();
                times = 1;
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldIgnoreMessageWhenStartMessageBeforeCreate(
            @Mocked final CommunicationEndpoint controlEndpoint, @Mocked final CommunicationEndpoint dataEndpoint)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final ControlMessage startMessage = new ControlMessage(RemoteMessageType.START);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                controlEndpoint.receiveMessage();
                returns(startMessage);
            }
        };

        proxy.attach();
        MessageListener messageListener = Deencapsulation.getField(proxy, "receiveMessageListener");
        messageListener.executeControlMessage();
        messageListener.executeDataMessage();

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldCallModuleDestroy(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint, @Mocked final TestModuleImplementsInterface module)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final ControlMessage destroyMessage = new ControlMessage(RemoteMessageType.DESTROY);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                controlEndpoint.receiveMessage();
                returns(createMessage, destroyMessage);

                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;

                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;
                dataEndpoint.receiveMessage();
                result = null;

                new TestModuleImplementsInterface();
                result = module;
            }
        };

        proxy.attach();
        Deencapsulation.setField(proxy, Executors.newSingleThreadScheduledExecutor());

        MessageListener messageListener = Deencapsulation.getField(proxy, "receiveMessageListener");
        messageListener.executeControlMessage();
        messageListener.executeDataMessage();

        messageListener.executeControlMessage();
        messageListener.executeDataMessage();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;

                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;

                new TestModuleImplementsInterface();
                times = 1;

                module.destroy();
                times = 1;
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void attachShouldDestroyWhenDestroyMessageBeforeCreate(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint)
            throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final ControlMessage destroyMessage = new ControlMessage(RemoteMessageType.DESTROY);

        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                controlEndpoint.receiveMessage();
                returns(destroyMessage);
            }
        };

        proxy.attach();
        MessageListener messageListener = Deencapsulation.getField(proxy, "receiveMessageListener");
        messageListener.executeControlMessage();
        messageListener.executeDataMessage();

        assertFalse(proxy.isAttached());
    }

    @Test
    public void attachShouldReceiveDataMessage(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint, @Mocked final TestModuleImplementsInterface module,
            @Mocked final MessageSerializer serializer) throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final ControlMessage startMessage = new ControlMessage(RemoteMessageType.START);
        final byte[] content = new byte[] { (byte) 0xA1, (byte) 0x60 };
        final DataMessage dataMessage = new DataMessage(content);

        new Expectations(RemoteModuleProxy.class) {
            {
                proxy.startListening();
            }
        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;

                new TestModuleImplementsInterface();
                result = module;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                returns(createMessage, startMessage);
                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;

                serializer.serializeMessage(RemoteModuleResultCode.OK.getValue(), version);

                dataEndpoint.connect();
                dataEndpoint.receiveMessage();
                returns(null, dataMessage);
            }
        };

        proxy.attach();
        RemoteModuleProxy.MessageListener receiveMessage = Deencapsulation.getField(proxy, "receiveMessageListener");
        receiveMessage.executeControlMessage();
        receiveMessage.executeDataMessage();

        receiveMessage.executeControlMessage();
        receiveMessage.executeDataMessage();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 1;

                dataEndpoint.connect();
                times = 1;

                module.create(anyLong, (Broker) any, anyString);
                times = 1;
                module.start();
                times = 1;
                module.receive(content);

                serializer.serializeMessage(RemoteModuleResultCode.OK.getValue(), version);
                times = 1;
            }
        };

        assertTrue(proxy.isAttached());
    }

    @Test
    public void detachSuccess(@Mocked final CommunicationEndpoint controlEndpoint,
            @Mocked final CommunicationEndpoint dataEndpoint, @Mocked final TestModuleImplementsInterface module,
            @Mocked final MessageSerializer serializer) throws ConnectionException, MessageDeserializationException {

        final RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        final DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final ControlMessage startMessage = new ControlMessage(RemoteMessageType.START);
        final byte[] content = new byte[] { (byte) 0xA1, (byte) 0x60 };
        final DataMessage dataMessage = new DataMessage(content);

//        new Expectations(RemoteModuleProxy.class) {
//            {
//                proxy.startListening();
//            }
//        };
        new Expectations() {
            {
                new CommunicationEndpoint(config.getIdentifier(), (CommunicationControlStrategy) any);
                result = controlEndpoint;
                new CommunicationEndpoint(dataSocketId, (CommunicationDataStrategy) any);
                result = dataEndpoint;

                new TestModuleImplementsInterface();
                result = module;

                controlEndpoint.connect();
                controlEndpoint.receiveMessage();
                returns(createMessage, startMessage, null);
                controlEndpoint.sendMessageAsync((byte[]) any);
                result = true;

                serializer.serializeMessage(RemoteModuleResultCode.OK.getValue(), version);
                serializer.serializeMessage(RemoteModuleResultCode.DETACH.getValue(), version);

                dataEndpoint.connect();
                dataEndpoint.receiveMessage();
                returns(null, dataMessage);
            }
        };

        proxy.attach();
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        proxy.detach();

        new Verifications() {
            {
                controlEndpoint.connect();
                times = 1;
                controlEndpoint.sendMessageAsync((byte[]) any);
                times = 2;

                dataEndpoint.connect();
                times = 1;

                module.create(anyLong, (Broker) any, anyString);
                times = 1;
                module.start();
                times = 1;
                module.receive(content);
                module.destroy();
                times = 1;

                serializer.serializeMessage(RemoteModuleResultCode.OK.getValue(), version);
                times = 1;
                
                serializer.serializeMessage(RemoteModuleResultCode.DETACH.getValue(), version);
                times = 1;
            }
        };

        assertFalse(proxy.isAttached());
    }
}
