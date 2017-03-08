/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

class CreateMessage extends ControlMessage {

    private final DataEndpointConfig endpointsConfig;
    private final String args;
    private final int version;

    public CreateMessage(DataEndpointConfig endpointsConfig, String args, int version) {
        super(RemoteMessageType.CREATE);
        this.endpointsConfig = endpointsConfig;
        this.args = args;
        this.version = version;
    }

    public DataEndpointConfig getDataEndpoint() {
        return this.endpointsConfig;
    }

    public String getArgs() {
        return this.args;
    }

    public int getVersion() {
        return this.version;
    }
}
