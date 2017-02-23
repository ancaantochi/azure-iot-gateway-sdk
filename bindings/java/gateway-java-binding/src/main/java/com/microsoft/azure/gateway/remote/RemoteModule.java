package com.microsoft.azure.gateway.remote;

import com.microsoft.azure.gateway.core.IGatewayModule;

public interface RemoteModule {

	IGatewayModule getGatewayModule();

	ModuleConfiguration getConfig();
}