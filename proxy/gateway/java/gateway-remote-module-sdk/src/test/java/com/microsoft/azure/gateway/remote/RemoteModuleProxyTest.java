/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nanomsg.NanoLibrary;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.core.IGatewayModule;

import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;

public class RemoteModuleProxyTest {

    private final static String identifier = "test";
    private String dataSocketId = "data-test";
    private String args = "";
    private int version = 1;
    private static byte[] messageBuffer = new byte[10];
    private static ModuleConfiguration config;

    @BeforeClass
    public static void applySharedMockups() {
        ModuleConfiguration.Builder configBuilder = new ModuleConfiguration.Builder();
        configBuilder.setIdentifier(identifier);
        configBuilder.setModuleClass(TestModule.class);
        configBuilder.setModuleVersion(1);

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
                controlEndpoint.connect(); times = 1;
                proxy.startListening(); times = 1;
            }
        };
    }

    @Test
    public void attachShouldCreateModuleInstance(@Mocked final MessageDeserializer deserializer)
            throws ConnectionException, MessageDeserializationException {

        RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final RemoteMessage message = null;

        new Expectations() {
            {
                deserializer.deserialize(ByteBuffer.wrap(messageBuffer));
                returns(createMessage, message);
            }
        };

        proxy.attach();

        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean isAttached = Deencapsulation.getField(proxy, boolean.class);
        IGatewayModule module = Deencapsulation.getField(proxy, IGatewayModule.class);
        
        assertTrue(isAttached);
        assertTrue(module != null);
    }
    
    @Test
    public void attach(@Mocked final MessageDeserializer deserializer)
            throws ConnectionException, MessageDeserializationException {

        RemoteModuleProxy proxy = new RemoteModuleProxy(config);
        DataEndpointConfig endpointsConfig = new DataEndpointConfig(dataSocketId, 1);
        final CreateMessage createMessage = new CreateMessage(endpointsConfig, args, version);
        final StartMessage startMessage = new StartMessage();
        final DataMessage dataMessage = null;

        new Expectations() {
            {
                deserializer.deserialize(ByteBuffer.wrap(messageBuffer));
                returns(createMessage, dataMessage, startMessage);
            }
        };

        proxy.attach();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean isAttached = Deencapsulation.getField(proxy, boolean.class);
        IGatewayModule module = Deencapsulation.getField(proxy, IGatewayModule.class);

        assertTrue(isAttached);
        assertTrue(module != null);
    }

}
