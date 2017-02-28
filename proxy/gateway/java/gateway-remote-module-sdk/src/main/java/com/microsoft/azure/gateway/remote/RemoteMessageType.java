package com.microsoft.azure.gateway.remote;

enum RemoteMessageType {
	ERROR(0), CREATE(1), REPLY(2), START(3), DESTROY(4);

	private final int value;

	private RemoteMessageType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

}
