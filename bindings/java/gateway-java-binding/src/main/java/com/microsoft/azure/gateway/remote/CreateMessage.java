package com.microsoft.azure.gateway.remote;

import java.util.List;

class CreateMessage extends RemoteMessage {

    private final List<DataEndpointConfig> endpointsConfig;
    private final String args;
    private final int version;

    public CreateMessage(List<DataEndpointConfig> endpointsConfig, String args, int version) {
        this.endpointsConfig = endpointsConfig;
        this.args = args;
        this.version = version;
    }

    public List<DataEndpointConfig> getDataEndpoints() {
        return endpointsConfig;
    }

    public String getArgs() {
        return args;
    }

    public int getVersion() {
        return version;
    }
}
