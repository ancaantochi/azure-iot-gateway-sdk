package com.microsoft.azure.gateway.remote;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class MessageSerializer {

    // 0xA1 comes from (A)zure (I)oT
	private static final byte FIRST_MESSAGE_BYTE = (byte) 0xA1;
	// 0x6C comes from (G)ateway control message
	private static final byte SECOND_MESSAGE_BYTE = (byte) 0x6C; 

	public byte[] serializeCreateCompleted(boolean success, int version) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		// Write Header
		try {
			dos.writeByte(FIRST_MESSAGE_BYTE);
			dos.writeByte(SECOND_MESSAGE_BYTE);
			dos.writeByte(version);
			dos.writeInt(RemoteMessageType.REPLY.getValue());
			dos.writeInt(2);

			// Write content
			dos.writeByte(success ? 0 : 1);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return bos.toByteArray();
	}

}
