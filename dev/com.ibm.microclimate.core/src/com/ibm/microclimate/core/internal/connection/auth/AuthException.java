package com.ibm.microclimate.core.internal.connection.auth;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ErrorResponse;

public class AuthException extends Exception {
	private static final long serialVersionUID = -3179235942807499803L;
	
	public AuthException(String msg) {
		super(msg);
	}
	
	public AuthException(ErrorObject err) {
		this(err.getHTTPStatusCode() + ": " + err.getDescription());
	}
	
	public AuthException(ErrorResponse errRes) {
		this(errRes.getErrorObject());
	}
}
