/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

class ControlMessage extends RemoteMessage {

    private RemoteMessageType messageType;

    public ControlMessage(RemoteMessageType type) {
        this.messageType = type;
    }

    public RemoteMessageType getMessageType() {
        return messageType;
    }

}
