package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

class CommunicationDataEndpoint extends CommunicationEndpoint {

	private final int type;

	public CommunicationDataEndpoint(String identifier, int type) {
		super(identifier);
		this.type = type;
	}

	@Override
	protected void createEndpoint() throws ConnectionException {
		this.endpointId = nano.nn_connect(this.socket, this.uri);

		if (this.endpointId < 0) {
			int errn = nano.nn_errno();
			throw new ConnectionException(String.format("Error: %d - %s\n", errn, nano.nn_strerror(errn)));
		}

	}

	@Override
	protected int getEndpointType() {
		return type;
	}

	@Override
	protected RemoteMessage deserializeMessage(ByteBuffer messageBuffer) throws MessageDeserializationException {
		return new DataMessage(messageBuffer.array());
	}
}
