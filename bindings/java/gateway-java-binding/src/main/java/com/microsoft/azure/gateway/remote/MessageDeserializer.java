package com.microsoft.azure.gateway.remote;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class MessageDeserializer {

	private static final byte FIRST_MESSAGE_BYTE = (byte) 0xA1; /*
																 * 0xA1 comes
																 * from (A)zure
																 * (I)oT
																 */
	private static final byte SECOND_MESSAGE_BYTE = (byte) 0x6C; /*
																	 * 0x6C
																	 * comes
																	 * from
																	 * (G)ateway
																	 * control
																	 * message
																	 */

	public RemoteMessage deserialize(ByteBuffer messageBuffer) throws MessageDeserializationException {
		RemoteMessageType msgType = this.deserializeHeader(messageBuffer);

		switch (msgType) {
		case CREATE:
			return this.deserializeCreateMessage(messageBuffer);
		case START:
			return this.deserializeStartMessage(messageBuffer);
		case DESTROY:
			return this.deserializeDestroyMessage(messageBuffer);
		case ERROR:
			return this.deserializeErrorMessage(messageBuffer);
		default:
			return new RemoteMessage();
		}
	}

	private RemoteMessageType deserializeHeader(ByteBuffer buffer) throws MessageDeserializationException {
		RemoteMessageType msgType = null;

		byte header1 = buffer.get();
		byte header2 = buffer.get();
		if (header1 == FIRST_MESSAGE_BYTE && header2 == SECOND_MESSAGE_BYTE) {
			// TODO: check version
			byte version = buffer.get();
			byte type = buffer.get();
			int totalSize = buffer.getInt();
			if (totalSize < 20) {
				throw new MessageDeserializationException("Invalid size");
			}

			msgType = RemoteMessageType.values()[type];

		} else {
			throw new MessageDeserializationException("Invalid message header");
		}

		return msgType;
	}

	private RemoteMessage deserializeCreateMessage(ByteBuffer buffer) throws MessageDeserializationException {
		CreateMessage message = null;

		int uriCount = buffer.getInt();
		List<DataEndpointConfig> endpointsConfig = new ArrayList<DataEndpointConfig>();

		try {
			for (int i = 0; i < uriCount; i++) {
				byte uriType = buffer.get();
				byte uriSize = buffer.get();
				
				String id = readNullTerminatedString(buffer);
				endpointsConfig.add(new DataEndpointConfig(id, uriType));
			}

			int argsSize = buffer.getInt();
			String[] args = new String[argsSize];
			for (int i = 0; i < argsSize; i++) {
				args[i] = readNullTerminatedString(buffer);
			}
			
			// TODO: get module args
			message = new CreateMessage(endpointsConfig, args[0]);
		} catch (IOException e) {
			throw new MessageDeserializationException(e);
		}

		return message;
	}

	private RemoteMessage deserializeDestroyMessage(ByteBuffer messageBuffer) {
		return new DestroyMessage();
	}

	private RemoteMessage deserializeStartMessage(ByteBuffer messageBuffer) {
		return new StartMessage();
	}

	private RemoteMessage deserializeErrorMessage(ByteBuffer messageBuffer) {
		// TODO Auto-generated method stub
		return null;
	}

	private static String readNullTerminatedString(ByteBuffer bis) throws IOException {
		ArrayList<Byte> byteArray = new ArrayList<Byte>();
		byte b = bis.get();

		while (b != '\0' && b != -1) {
			byteArray.add(b);
			b = bis.get();
		}

		byte[] result;

		if (b != -1) {

			result = new byte[byteArray.size()];
			for (int index = 0; index < result.length; index++) {
				result[index] = byteArray.get(index);
			}
		} else {
			throw new IOException("Could not read null-terminated string.");
		}

		return new String(result);
	}

}
