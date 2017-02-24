package com.microsoft.azure.gateway.remote;

class CreateMessage extends RemoteMessage {

    private final DataEndpointConfig endpointsConfig;
    private final String args;
    private final int version;

    public CreateMessage(DataEndpointConfig endpointsConfig, String args, int version) {
        this.endpointsConfig = endpointsConfig;
        this.args = args;
        this.version = version;
    }

    public DataEndpointConfig getDataEndpoints() {
        return endpointsConfig;
    }

    public String getArgs() {
        return args;
    }

    public int getVersion() {
        return version;
    }
}
