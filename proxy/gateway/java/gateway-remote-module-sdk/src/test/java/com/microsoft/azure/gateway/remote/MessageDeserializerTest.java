/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;

import org.junit.Test;

import mockit.Deencapsulation;

public class MessageDeserializerTest {

    private static final String MODULE_ARGS = "module args\0";
    private static final String DATA_MESSAGE_SOCKET_NAME = "data_message\0";
    private static final byte CREATE_MESSAGE_TYPE = (byte) RemoteMessageType.CREATE.getValue();
    private static final byte INVALID_MESSAGE_TYPE = (byte) 5;
    private static final byte VALID_MESSAGE_VERSION = CREATE_MESSAGE_TYPE;
    private static final byte INVALID_MESSAGE_VERSION = (byte) 0x05;
    private static final byte VALID_HEADER2 = (byte) 0x6C;
    private static final byte VALID_HEADER1 = (byte) 0xA1;
    private static final byte INVALID_HEADER2 = (byte) 0x6A;
    private static final byte INVALID_HEADER1 = (byte) 0xA2;
    private static final byte VALID_URI_TYPE = (byte) 16;
    private static final int INVALID_URI_SIZE_TOO_SMALL = 5;
    private static final int INVALID_URI_SIZE_TOO_LARGE = 30;
    private byte smallSize = 7;

    @Test
    public void deserializationShouldReturnCreateMessage() throws MessageDeserializationException {
        int size = 21 + DATA_MESSAGE_SOCKET_NAME.length() + MODULE_ARGS.length();
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(size);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(CREATE_MESSAGE_TYPE);
        invalidSizeMessage.putInt(size);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(VALID_URI_TYPE);
        invalidSizeMessage.putInt(DATA_MESSAGE_SOCKET_NAME.length());
        invalidSizeMessage.put(DATA_MESSAGE_SOCKET_NAME.getBytes());
        invalidSizeMessage.putInt(MODULE_ARGS.length());
        invalidSizeMessage.put(MODULE_ARGS.getBytes());

        MessageDeserializer deserializer = new MessageDeserializer();
        RemoteMessage message = deserializer.deserialize(invalidSizeMessage);
        assertTrue(message instanceof CreateMessage);
        CreateMessage createMessage = (CreateMessage) message;
        assertEquals(createMessage.getDataEndpoint().getId(), DATA_MESSAGE_SOCKET_NAME.trim());
        assertEquals(createMessage.getDataEndpoint().getType(), VALID_URI_TYPE);
        assertEquals(createMessage.getVersion(), VALID_MESSAGE_VERSION);
        assertEquals(createMessage.getArgs(), MODULE_ARGS.trim());
    }

    @Test
    public void deserializationShouldReturnStartMessage() throws MessageDeserializationException {
        int size = 8;
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(size);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put((byte) RemoteMessageType.START.getValue());
        invalidSizeMessage.putInt(size);

        MessageDeserializer deserializer = new MessageDeserializer();
        ControlMessage message = (ControlMessage) deserializer.deserialize(invalidSizeMessage);
        assertEquals(RemoteMessageType.START, message.getMessageType());
    }

    @Test
    public void deserializationShouldReturnDestroyMessage() throws MessageDeserializationException {
        int size = 8;
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(size);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put((byte) RemoteMessageType.DESTROY.getValue());
        invalidSizeMessage.putInt(size);

        MessageDeserializer deserializer = new MessageDeserializer();
        ControlMessage message = (ControlMessage) deserializer.deserialize(invalidSizeMessage);
        assertEquals(RemoteMessageType.DESTROY, message.getMessageType());
    }

    @Test
    public void deserializationShouldReturnNullWhenMessageTypeIsError() throws MessageDeserializationException {
        int size = 8;
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(size);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put((byte) RemoteMessageType.ERROR.getValue());
        invalidSizeMessage.putInt(size);

        MessageDeserializer deserializer = new MessageDeserializer();
        RemoteMessage message = deserializer.deserialize(invalidSizeMessage);
        assertNull(message);
    }

    @Test
    public void deserializationShouldThrowWhenInvalidHeader() {
        ByteBuffer invalidHeader1Message = ByteBuffer.allocate(8);
        invalidHeader1Message.put(INVALID_HEADER1);
        invalidHeader1Message.put(INVALID_HEADER2);

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidHeader1Message);
        } catch (MessageDeserializationException e) {
            assertEquals(e.getMessage(), "Invalid message header.");
        }

        ByteBuffer invalidHeader2Message = ByteBuffer.allocate(8);
        invalidHeader2Message.put(VALID_HEADER1);
        invalidHeader2Message.put(INVALID_HEADER2);

        try {
            deserializer.deserialize(invalidHeader2Message);
        } catch (MessageDeserializationException e) {
            assertEquals(e.getMessage(), "Invalid message header.");
        }
    }

    @Test
    public void deserializationShouldThrowWhenInvalidMessageVersion() {
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(8);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(INVALID_MESSAGE_VERSION);

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            assertEquals(e.getMessage(), "Version is not supported");
        }
    }

    @Test
    public void deserializationShouldThrowWhenInvalidMessageType() {
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(8);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(INVALID_MESSAGE_TYPE);

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            assertEquals(e.getMessage(), "Invalid message type.");
        }
    }
    
    @Test
    public void deserializationShouldThrowWhenHeaderIsMissing() {
       
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(1);
        invalidSizeMessage.put(VALID_HEADER1);

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            byte minSize = Deencapsulation.getField(MessageDeserializer.class, "BASE_MESSAGE_SIZE");
            assertEquals(e.getMessage(), String.format("Message size %s should be >= %s", invalidSizeMessage.limit(), minSize));
        }

        invalidSizeMessage = ByteBuffer.allocate(0);

        deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            byte minSize = Deencapsulation.getField(MessageDeserializer.class, "BASE_MESSAGE_SIZE");
            assertEquals(e.getMessage(), String.format("Message size %s should be >= %s", invalidSizeMessage.limit(), minSize));
        }
    }

    @Test
    public void deserializationShouldThrowWhenInvalidMessageSize() {
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(7);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(CREATE_MESSAGE_TYPE);

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            byte minSize = Deencapsulation.getField(MessageDeserializer.class, "BASE_MESSAGE_SIZE");
            assertEquals(e.getMessage(), String.format("Message size %s should be >= %s", smallSize, minSize));
        }

        invalidSizeMessage = ByteBuffer.allocate(8);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(CREATE_MESSAGE_TYPE);
        invalidSizeMessage.putInt(64);

        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            assertEquals(e.getMessage(), String.format("Message size in header %s is different that actual size %s", 64,
                    invalidSizeMessage.limit()));
        }
    }

    @Test
    public void deserializationShouldThrowWhenInvalidCreateSize() {
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(8);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(CREATE_MESSAGE_TYPE);
        invalidSizeMessage.putInt(8);

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            byte minSize = Deencapsulation.getField(MessageDeserializer.class, "BASE_CREATE_SIZE");
            assertEquals(e.getMessage(), String.format("Create message size %s should be >= %s", 8, minSize));
        }
    }

    @Test
    public void deserializationShouldThrowWhenInvalidUriSizeTooSmall() {
        int size = 17 + DATA_MESSAGE_SOCKET_NAME.length();
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(size);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(CREATE_MESSAGE_TYPE);
        invalidSizeMessage.putInt(size);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(VALID_URI_TYPE);
        invalidSizeMessage.putInt(INVALID_URI_SIZE_TOO_SMALL);
        invalidSizeMessage.put(DATA_MESSAGE_SOCKET_NAME.getBytes());

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            assertEquals(e.getMessage(), "Can not deserialize string arguments.");
        }
    }

    @Test
    public void deserializationShouldThrowWhenInvalidUriSizeTooLarge() {
        int size = 17 + DATA_MESSAGE_SOCKET_NAME.length();
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(size);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(CREATE_MESSAGE_TYPE);
        invalidSizeMessage.putInt(size);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(VALID_URI_TYPE);
        invalidSizeMessage.putInt(INVALID_URI_SIZE_TOO_LARGE);
        invalidSizeMessage.put(DATA_MESSAGE_SOCKET_NAME.getBytes());

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            assertEquals(e.getMessage(), "Can not deserialize string arguments.");
        }
    }

    @Test
    public void deserializationShouldThrowWhenInvalidArgsSizeTooLarge() {
        int size = 21 + DATA_MESSAGE_SOCKET_NAME.length() + MODULE_ARGS.length();
        ByteBuffer invalidSizeMessage = ByteBuffer.allocate(size);
        invalidSizeMessage.put(VALID_HEADER1);
        invalidSizeMessage.put(VALID_HEADER2);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(CREATE_MESSAGE_TYPE);
        invalidSizeMessage.putInt(size);
        invalidSizeMessage.put(VALID_MESSAGE_VERSION);
        invalidSizeMessage.put(VALID_URI_TYPE);
        invalidSizeMessage.putInt(DATA_MESSAGE_SOCKET_NAME.length());
        invalidSizeMessage.put(DATA_MESSAGE_SOCKET_NAME.getBytes());
        invalidSizeMessage.putInt(INVALID_URI_SIZE_TOO_LARGE);
        invalidSizeMessage.put(MODULE_ARGS.getBytes());

        MessageDeserializer deserializer = new MessageDeserializer();
        try {
            deserializer.deserialize(invalidSizeMessage);
        } catch (MessageDeserializationException e) {
            assertEquals(e.getMessage(), "Can not deserialize string arguments.");
        }
    }
}
