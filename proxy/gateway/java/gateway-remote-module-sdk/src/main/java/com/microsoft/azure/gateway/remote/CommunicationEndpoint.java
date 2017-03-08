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
    private byte version;
    private int socket;
    private int endpointId;

    public CommunicationEndpoint(String identifier, CommunicationStrategy communicationStrategy) {
        if (identifier == null)
            throw new IllegalArgumentException("Idenitfier can not be null");
        if (communicationStrategy == null)
            throw new IllegalArgumentException("Communication strategy can not be null");

        this.communicationStrategy = communicationStrategy;
        this.nano = new NanoLibrary();
        this.uri = communicationStrategy.getEndpointUri(identifier);
    }
    
    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public void connect() throws ConnectionException {
        this.createSocket();
        this.createEndpoint();
    }

    public RemoteMessage receiveMessage() throws ConnectionException, MessageDeserializationException {

        byte[] messageBuffer = this.nano.nn_recvbyte(socket, nano.NN_DONTWAIT);

        if (messageBuffer == null) {
            int errn = this.nano.nn_errno();
            if (errn == this.nano.EAGAIN) {
                return null;
            } else {
                throw new ConnectionException(String.format("Error: %d - %s\n", errn, this.nano.nn_strerror(errn)));
            }
        }

        return this.communicationStrategy.deserializeMessage(ByteBuffer.wrap(messageBuffer), version);
    }

    public void disconnect() {
        this.nano.nn_shutdown(socket, endpointId);
        this.nano.nn_close(socket);
    }

    public void sendMessage(byte[] message) throws ConnectionException {
        int result = this.nano.nn_sendbyte(socket, message, 0);
        if (result < 0) {
            int errn = this.nano.nn_errno();
            throw new ConnectionException(String.format("Error: %d - %s\n", errn, this.nano.nn_strerror(errn)));
        }
    }

    public boolean sendMessageAsync(byte[] message) throws ConnectionException {
        int result = this.nano.nn_sendbyte(socket, message, this.nano.NN_DONTWAIT);
        if (result < 0) {
            int errn = this.nano.nn_errno();
            if (errn == this.nano.EAGAIN) {
                return false;
            } else {
                throw new ConnectionException(String.format("Error: %d - %s\n", errn, this.nano.nn_strerror(errn)));
            }
        }
        return true;
    }

    public boolean isControlEndpoint() {
        return (this.communicationStrategy instanceof CommunicationControlStrategy);
    }

    private void createSocket() throws ConnectionException {
        this.socket = this.nano.nn_socket(this.nano.AF_SP, this.communicationStrategy.getEndpointType(this.nano));
        if (this.socket < 0) {
            throw new ConnectionException(String.format("Error in nn_socket: %s\n", this.nano.nn_strerror(this.nano.nn_errno())));
        }
    }

    private void createEndpoint() throws ConnectionException {
        this.endpointId = this.nano.nn_bind(this.socket, this.uri);

        if (this.endpointId < 0) {
            int errn = this.nano.nn_errno();
            this.nano.nn_close(this.socket);
            throw new ConnectionException(String.format("Error: %d - %s\n", errn, this.nano.nn_strerror(errn)));
        }
    }
}
