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
        this.uri = String.format("ipc:///%s.ipc", identifier);
        this.nano = new NanoLibrary();
        this.communicationStrategy = communicationStrategy;
    }
    
    public void connect() throws ConnectionException {
        createSocket();
        this.endpointId = this.communicationStrategy.createEndpoint(nano, this.socket, this.uri);
    }

    public RemoteMessage receiveMessage() throws ConnectionException, MessageDeserializationException {

        byte[] messageBuffer = nano.nn_recvbyte(this.socket, nano.NN_DONTWAIT);

        if (messageBuffer == null) {
            int errn = nano.nn_errno();
            errn = nano.nn_errno();
            if (errn == nano.EAGAIN) {
                return null;
            } else {
                throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
            }
        }

        return this.communicationStrategy.deserializeMessage(ByteBuffer.wrap(messageBuffer));
    }

    public void disconnect() {
        nano.nn_shutdown(socket, endpointId);
    }

    private void createSocket() throws ConnectionException {
        socket = nano.nn_socket(nano.AF_SP, this.communicationStrategy.getEndpointType(nano));
        if (socket < 0) {
            throw new ConnectionException(String.format("Error in nn_socket: %s\n", nano.nn_strerror(nano.nn_errno())));
        }
    }

    public void sendMessage(byte[] message) throws ConnectionException {
        int result = nano.nn_sendbyte(socket, message, 0);
        if (result < 0) {
            int errn = nano.nn_errno();
            throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
        }
    }

    public boolean sendMessageDontWait(byte[] message) throws ConnectionException {
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
}
