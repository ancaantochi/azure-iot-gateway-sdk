/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

import org.nanomsg.NanoLibrary;

class CommunicationControlStrategy implements CommunicationStrategy {

    @Override
    public int getEndpointType(NanoLibrary nano) {
        return nano.NN_PAIR;
    }

    @Override
    public RemoteMessage deserializeMessage(ByteBuffer messageBuffer, byte version) throws MessageDeserializationException {
        MessageDeserializer deserializer = new MessageDeserializer();
        return deserializer.deserialize(messageBuffer, version);
    }

    @Override
    public String getEndpointUri(String identifier) {
        return String.format("ipc://%s.ipc", identifier);
    }
}
