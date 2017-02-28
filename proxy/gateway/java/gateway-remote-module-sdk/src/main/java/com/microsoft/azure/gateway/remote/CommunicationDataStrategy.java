package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

import org.nanomsg.NanoLibrary;

class CommunicationDataStrategy implements CommunicationStrategy {

	private final int type;
	
	public CommunicationDataStrategy(int type) {
	    this.type = type;
    }

	@Override
	public int createEndpoint(NanoLibrary nano, int socket, String uri) throws ConnectionException {
		int endpointId = nano.nn_connect(socket, uri);

		if (endpointId < 0) {
			int errn = nano.nn_errno();
			throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
		}

		return endpointId;
	}

	@Override
    public int getEndpointType(NanoLibrary nano) {
		return type;
	}

	@Override
    public RemoteMessage deserializeMessage(ByteBuffer messageBuffer) throws MessageDeserializationException {
		return new DataMessage(messageBuffer.array());
	}
}
