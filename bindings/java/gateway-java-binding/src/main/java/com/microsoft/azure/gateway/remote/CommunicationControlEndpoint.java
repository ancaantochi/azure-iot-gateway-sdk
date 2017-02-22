package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

class CommunicationControlEndpoint extends CommunicationEndpoint {

	public CommunicationControlEndpoint(String identifier) {
		super(identifier);
	}

	@Override
	protected void createEndpoint() throws ConnectionException {
		endpointId = nano.nn_bind(socket, this.uri);

		if (endpointId < 0) {
			int errn = nano.nn_errno();
			throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
		}
	}

	@Override
	protected int getEndpointType() {
		return nano.NN_PAIR;
	}

	@Override
	protected RemoteMessage deserializeMessage(ByteBuffer messageBuffer) throws MessageDeserializationException {
		MessageDeserializer deserializer = new MessageDeserializer();
		return deserializer.deserialize(messageBuffer);
	}

	public void sendCreateReply() throws ConnectionException {
		// TODO: get version
		int version = 1;
		byte[] message = new MessageSerializer().serializeCreateComplete(true, version);
		
		super.sendMessage(message);
	}
}
