/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

enum RemoteModuleResultCode {
    DETACH(-1), OK(0), CONNECTION_ERROR(1), CREATION_ERROR(2);

    private final int value;

    private RemoteModuleResultCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
