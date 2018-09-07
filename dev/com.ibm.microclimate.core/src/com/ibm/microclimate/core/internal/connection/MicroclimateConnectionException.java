package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;

/**
 * Custom exception to indicate that connecting to the Microclimate Socket at the given URL failed.
 *
 * @author timetchells@ibm.com
 */
public class MicroclimateConnectionException extends IOException {
	private static final long serialVersionUID = -7026779560626815421L;

	public final String connectionUrl;

	public final String message;

	public MicroclimateConnectionException(String url) {
		String msg = String.format("Connecting to Microclimate Socket at \"%s\" failed", url);

		message = msg;
		connectionUrl = url;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
