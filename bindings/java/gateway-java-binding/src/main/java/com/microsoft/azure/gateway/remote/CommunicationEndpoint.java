package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

import org.nanomsg.NanoLibrary;

abstract class CommunicationEndpoint {

    protected final String uri;
    protected final NanoLibrary nano;
    protected int socket;
    protected int endpointId;

    public CommunicationEndpoint(String identifier) {
        this.uri = String.format("ipc:///%s.ipc", identifier);
        this.nano = new NanoLibrary();
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

        return deserializeMessage(ByteBuffer.wrap(messageBuffer));
    }

    public void connect() throws ConnectionException {
        createSocket();
        createEndpoint();
    }

    public void disconnect() throws ConnectionException {
        int result = nano.nn_shutdown(socket, endpointId);

        if (result < 0) {
            int errn = nano.nn_errno();
            throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
        }
    }

    protected abstract RemoteMessage deserializeMessage(ByteBuffer messageBuffer)
            throws MessageDeserializationException;

    protected abstract void createEndpoint() throws ConnectionException;

    protected abstract int getEndpointType();

    private void createSocket() throws ConnectionException {
        socket = nano.nn_socket(nano.AF_SP, getEndpointType());
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
