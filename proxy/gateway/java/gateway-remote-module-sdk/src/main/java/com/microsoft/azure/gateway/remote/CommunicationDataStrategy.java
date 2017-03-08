/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import java.nio.ByteBuffer;

import org.nanomsg.NanoLibrary;

class CommunicationDataStrategy implements CommunicationStrategy {

	private final int type;
	
	public CommunicationDataStrategy(int type) {
	    this.type = type;
    }

	@Override
    public int getEndpointType(NanoLibrary nano) {
		return this.type;
	}

	@Override
    public RemoteMessage deserializeMessage(ByteBuffer messageBuffer) throws MessageDeserializationException {
		return new DataMessage(messageBuffer.array());
	}

    @Override
    public String getEndpointUri(String identifier) {
        return identifier;
    }
}
