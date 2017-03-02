/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

import org.nanomsg.NanoLibrary;

class CommunicationEndpoint {

    private final String uri;
    private final NanoLibrary nano;
    private final CommunicationStrategy communicationStrategy;
    private int socket;
    private int endpointId;

    public CommunicationEndpoint(String identifier, CommunicationStrategy communicationStrategy) {
        this.communicationStrategy = communicationStrategy;
        this.nano = new NanoLibrary();
        this.uri = communicationStrategy.getEndpointUri(identifier);
    }

    public void connect() throws ConnectionException {
        this.createSocket();
        this.createEndpoint();
    }

    public RemoteMessage receiveMessage() throws ConnectionException, MessageDeserializationException {

        byte[] messageBuffer = nano.nn_recvbyte(socket, nano.NN_DONTWAIT);

        if (messageBuffer == null) {
            int errn = nano.nn_errno();
            errn = nano.nn_errno();
            if (errn == nano.EAGAIN) {
                return null;
            } else {
                throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
            }
        }

        return communicationStrategy.deserializeMessage(ByteBuffer.wrap(messageBuffer));
    }

    public void disconnect() {
        nano.nn_shutdown(socket, endpointId);
        nano.nn_close(socket);
    }

    public void sendMessage(byte[] message) throws ConnectionException {
        int result = nano.nn_sendbyte(socket, message, 0);
        if (result < 0) {
            int errn = nano.nn_errno();
            throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
        }
    }

    public boolean sendMessageAsync(byte[] message) throws ConnectionException {
        int result = nano.nn_sendbyte(socket, message, nano.NN_DONTWAIT);
        if (result < 0) {
            int errn = nano.nn_errno();
            if (errn == nano.EAGAIN) {
                return false;
            } else {
                throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
            }
        }
        return true;
    }

    public boolean isControlEndpoint() {
        return (communicationStrategy instanceof CommunicationControlStrategy);
    }

    private void createSocket() throws ConnectionException {
        socket = nano.nn_socket(nano.AF_SP, communicationStrategy.getEndpointType(nano));
        if (socket < 0) {
            throw new ConnectionException(String.format("Error in nn_socket: %s\n", nano.nn_strerror(nano.nn_errno())));
        }
    }

    private void createEndpoint() throws ConnectionException {
        endpointId = nano.nn_bind(socket, uri);

        if (endpointId < 0) {
            int errn = nano.nn_errno();
            nano.nn_close(socket);
            throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
        }
    }
}
