package com.microsoft.azure.gateway.remote;

public class ModuleInstantionException extends Exception {

	private static final long serialVersionUID = -8910366313876604839L;

	public ModuleInstantionException() {
		super();
	}

	public ModuleInstantionException(String message) {
		super(message);
	}

	public ModuleInstantionException(Throwable cause) {
		super(cause);
	}

	public ModuleInstantionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ModuleInstantionException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
