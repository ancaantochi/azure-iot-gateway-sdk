package com.microsoft.azure.gateway.remote;

public class ConnectionException extends Exception {

	private static final long serialVersionUID = 9084269424900889737L;

	public ConnectionException() {
		super();
	}

	public ConnectionException(String message) {
		super(message);
	}

	public ConnectionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionException(Throwable cause) {
		super(cause);
	}
}