package com.microsoft.azure.gateway.remote;

class DataMessage extends RemoteMessage {

	private final byte[] content;

	public DataMessage(byte[] content) {
		this.content = content;
	}

	public byte[] getContent() {
		return content;
	}
}
