/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import java.io.IOException;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.messaging.Message;

class BrokerProxy extends Broker {
    private final static long emptyAddress = 0;
    private final CommunicationEndpoint endpoint;

    public BrokerProxy(CommunicationEndpoint dataEndpoint) {
        super(emptyAddress);

        if (dataEndpoint == null)
            throw new IllegalArgumentException("Communication endpoint was not initialized.");

        this.endpoint = dataEndpoint;
    }

    @Override
    public int publishMessage(Message message, long moduleAddr) throws IOException {
        try {
            this.endpoint.sendMessage(message.toByteArray());
        } catch (ConnectionException e) {
            throw new IOException(e);
        }
        return 0;
    }
}
