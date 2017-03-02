/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

import org.nanomsg.NanoLibrary;

interface CommunicationStrategy {
    RemoteMessage deserializeMessage(ByteBuffer buffer) throws MessageDeserializationException;
    int getEndpointType(NanoLibrary nano);
    String getEndpointUri(String identifier);
}
