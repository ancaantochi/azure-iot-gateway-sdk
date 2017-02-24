package com.microsoft.azure.gateway.remote;

class DataEndpointConfig {
	private final String id;
	private final int type;

	public DataEndpointConfig(String id, int type) {
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public int getType() {
		return type;
	}
}
