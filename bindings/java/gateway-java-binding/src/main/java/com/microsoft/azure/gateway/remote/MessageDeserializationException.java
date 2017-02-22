package com.microsoft.azure.gateway.remote;

public class MessageDeserializationException extends Exception {

	private static final long serialVersionUID = 375190278270633771L;

	public MessageDeserializationException() {
		super();
	}

	public MessageDeserializationException(String message) {
		super(message);
	}

	public MessageDeserializationException(Throwable cause) {
		super(cause);
	}

	public MessageDeserializationException(String message, Throwable cause) {
		super(message, cause);
	}

	public MessageDeserializationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
