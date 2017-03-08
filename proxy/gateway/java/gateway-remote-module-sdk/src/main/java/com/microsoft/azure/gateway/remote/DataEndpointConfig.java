/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

class DataEndpointConfig {
	private final String id;
	private final int type;

	public DataEndpointConfig(String id, int type) {
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return this.id;
	}

	public int getType() {
		return this.type;
	}
}
