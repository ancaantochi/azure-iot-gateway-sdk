package com.microsoft.azure.gateway.remote;

import java.util.List;

class CreateMessage extends RemoteMessage {

	private final List<DataEndpointConfig> endpointsConfig;
	private final String args;
	public CreateMessage(List<DataEndpointConfig> endpointsConfig, String args) {
		this.endpointsConfig = endpointsConfig;
		this.args = args;
	}

	public List<DataEndpointConfig> getDataEndpoints() {
		return endpointsConfig;
	}
	
	public String getArgs() {
		return args;
	}

}
