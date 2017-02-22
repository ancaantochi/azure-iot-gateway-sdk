package com.microsoft.azure.gateway.remote;

import java.io.IOException;

import com.microsoft.azure.gateway.core.Broker;
import com.microsoft.azure.gateway.messaging.Message;

class BrokerProxy extends Broker {
	private final static long emptyAddress = 0;
	private CommunicationEndpoint endpoint;
	
	public BrokerProxy() {
		super(emptyAddress);
	}
	
	public void setEndpoint(CommunicationEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public int publishMessage(Message message, long moduleAddr) throws IOException {
        if (this.endpoint == null)
        	throw new IllegalStateException("Communication endpoint was not initialized.");
        
        try {
			endpoint.sendMessage(message.toByteArray());
		} catch (ConnectionException e) {
			throw new IOException(e);
		}
        return 0;
    }

}
