/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

import mockit.Deencapsulation;

public class MessageSerializerTest {

    @Test
    public void test() {
        MessageSerializer serializer = new MessageSerializer();
        byte[] result = serializer.serializeMessage(1, 1);
        
        ByteBuffer buffer = ByteBuffer.wrap(result);
        byte header1 = buffer.get();
        byte header2 = buffer.get();
        
        byte version = buffer.get();
        byte messageType = buffer.get();
        int totalSize = buffer.getInt();
        byte status = buffer.get();
        
        assertEquals(header1, Deencapsulation.getField(MessageSerializer.class, "FIRST_MESSAGE_BYTE"));
        assertEquals(header2, Deencapsulation.getField(MessageSerializer.class, "SECOND_MESSAGE_BYTE"));
        assertEquals(version, 1);
        assertEquals(messageType, RemoteMessageType.REPLY.getValue());
        assertEquals(totalSize, buffer.limit());
        assertEquals(status, 1);
    }

}
