package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

import org.nanomsg.NanoLibrary;

class CommunicationControlStrategy implements CommunicationStrategy {

	@Override
	public int createEndpoint(NanoLibrary nano, int socket, String uri) throws ConnectionException {
		int endpointId = nano.nn_bind(socket, uri);

		if (endpointId < 0) {
			int errn = nano.nn_errno();
			throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
		}
		
		return endpointId;
	}

	@Override
    public int getEndpointType(NanoLibrary nano) {
		return nano.NN_PAIR;
	}

	@Override
    public RemoteMessage deserializeMessage(ByteBuffer messageBuffer) throws MessageDeserializationException {
		MessageDeserializer deserializer = new MessageDeserializer();
		return deserializer.deserialize(messageBuffer);
	}
}
