package com.microsoft.azure.gateway.remote;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import com.microsoft.azure.gateway.core.IGatewayModule;

class RemoteModuleImpl implements RemoteModule {

	private final ModuleConfiguration config;
	private List<CommunicationEndpoint> endpoints;
	private IGatewayModule module;
	private Future<?> future;

	RemoteModuleImpl(ModuleConfiguration configuration) {
		if (configuration == null)
			throw new IllegalArgumentException("Configuration can not be null.");

		this.config = configuration;
		this.endpoints = new ArrayList<CommunicationEndpoint>();
	}

	@Override
	public IGatewayModule getGatewayModule() {
		return this.module;
	}

	@Override
	public ModuleConfiguration getConfig() {
		return this.config;
	}

	void disconnectEndpoints() throws ConnectionException {
		for (CommunicationEndpoint endpoint : endpoints) {
			endpoint.disconnect();
		}
	}
	
	void addEndpoint(CommunicationEndpoint endpoint) {
		endpoints.add(endpoint);
	}

	Future<?> getFuture() {
		return future;
	}
	
	void setFuture(Future<?> future) {
		this.future = future;
	}

	List<CommunicationEndpoint> getEndpoints() {
		return endpoints;
	}
}
