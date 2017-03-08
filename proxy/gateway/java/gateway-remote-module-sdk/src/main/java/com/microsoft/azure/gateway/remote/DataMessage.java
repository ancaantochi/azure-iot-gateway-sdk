/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

class DataMessage extends RemoteMessage {

	private final byte[] content;

	public DataMessage(byte[] content) {
		this.content = content;
	}

	public byte[] getContent() {
		return this.content;
	}
}
