/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

/**
 * An endpoint that is used to communicate with the remote Gateway which uses nanomsg to send and receive messages. 
 *
 */
class CommunicationEndpoint {

    private final String uri;
    private final NanomsgLibrary nano;
    private final CommunicationStrategy communicationStrategy;
    private byte version;
    private int socket;
    private int endpointId;

    public CommunicationEndpoint(String identifier, CommunicationStrategy communicationStrategy) {
        if (identifier == null)
            throw new IllegalArgumentException("Identifier can not be null");
        if (communicationStrategy == null)
            throw new IllegalArgumentException("Communication strategy can not be null");

        this.communicationStrategy = communicationStrategy;
        this.nano = new NanomsgLibrary();
        this.uri = communicationStrategy.getEndpointUri(identifier);
    }
    
    public byte getVersion() {
        return version;
    }

    /**
     * Set message version
     *
     * @param version
     */
    public void setVersion(byte version) {
        this.version = version;
    }

    /**
     * Creates a socket and connects to it.
     * 
     */
    public void connect() throws ConnectionException {
        this.createSocket();
        this.createEndpoint();
    }

    /**
     * Checks if there are new messages to receive. This method does not block, if there is no message it returns immediately.
     * 
     * @return Deserialized message
     *
     * @throws ConnectionException If there is any error receiving the message from the Gateway 
     * @throws MessageDeserializationException If the message is not in the expected format.
     */
    public RemoteMessage receiveMessage() throws ConnectionException, MessageDeserializationException {

        byte[] messageBuffer = this.nano.receiveMessageAsync(this.socket);
        if (messageBuffer == null) {
            return null;
        }
        return this.communicationStrategy.deserializeMessage(ByteBuffer.wrap(messageBuffer), version);
    }

    
    public void disconnect() {
        this.nano.shutdown(socket, endpointId);
        this.nano.closeSocket(socket);
    }

    public void sendMessage(byte[] message) throws ConnectionException {
       this.nano.sendMessage(socket, message);
        
    }

    public boolean sendMessageAsync(byte[] message) throws ConnectionException {
        return this.nano.sendMessageAsync(this.socket, message);
//        if (result < 0) {
//            int errn = this.nano.nn_errno();
//            if (errn == this.nano.EAGAIN) {
//                return false;
//            } else {
//                throw new ConnectionException(String.format("Error: %d - %s\n", errn, this.nano.nn_strerror(errn)));
//            }
//        }
//        return true;
    }

    public boolean isControlEndpoint() {
        return (this.communicationStrategy instanceof CommunicationControlStrategy);
    }

    private void createSocket() throws ConnectionException {
        this.socket = this.nano.createSocket(this.communicationStrategy.getEndpointType());
//        if (this.socket < 0) {
//            throw new ConnectionException(String.format("Error in nn_socket: %s\n", this.nano.nn_strerror(this.nano.nn_errno())));
//        }
    }

    private void createEndpoint() throws ConnectionException {
        this.endpointId = this.nano.bind(this.socket, this.uri);
//
//        if (this.endpointId < 0) {
//            int errn = this.nano.nn_errno();
//            this.nano.nn_close(this.socket);
//            throw new ConnectionException(String.format("Error: %d - %s\n", errn, this.nano.nn_strerror(errn)));
//        }
    }
}
