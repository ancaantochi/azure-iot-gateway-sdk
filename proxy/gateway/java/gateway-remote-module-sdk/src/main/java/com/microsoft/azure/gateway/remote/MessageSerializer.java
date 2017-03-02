/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */
package com.microsoft.azure.gateway.remote;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class MessageSerializer {

    // 0xA1 comes from (A)zure (I)oT
    private static final byte FIRST_MESSAGE_BYTE = (byte) 0xA1;
    // 0x6C comes from (G)ateway control message
    private static final byte SECOND_MESSAGE_BYTE = (byte) 0x6C;

    public byte[] serializeMessage(int status, int version) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // Write Header
            dos.writeByte(FIRST_MESSAGE_BYTE);
            dos.writeByte(SECOND_MESSAGE_BYTE);
            dos.writeByte(version);
            dos.writeByte(RemoteMessageType.REPLY.getValue());
            int totalSize = dos.size() + 5;
            dos.writeInt(totalSize);

            // Write content
            dos.writeByte(status);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bos.toByteArray();
    }
}
