package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

import org.nanomsg.NanoLibrary;

interface CommunicationStrategy {
    RemoteMessage deserializeMessage(ByteBuffer buffer) throws MessageDeserializationException;

    int createEndpoint(NanoLibrary nano, int socket, String uri) throws ConnectionException;

    int getEndpointType(NanoLibrary nano);
}
