package com.ibm.microclimate.core.internal.connection.auth;

import org.eclipse.urischeme.IUriSchemeHandler;
import org.eclipse.swt.widgets.Display;
import org.eclipse.jface.dialogs.MessageDialog;

import com.ibm.microclimate.core.internal.MCLogger;

@SuppressWarnings("restriction")
public class AuthCallbackHandler implements IUriSchemeHandler {

	public AuthCallbackHandler() {
		MCLogger.log("Activating AuthCallbackHandler");
	}

	@Override
	public void handle(String uri) {
		MCLogger.log("Handling URI: " + uri);

		if (uri.startsWith(Authenticator.CALLBACK_URI)) {
			try {
				Authenticator.instance().handleAuthorizationCallback(uri);
			} catch (Exception e) {
				MCLogger.logError("Error handling auth callback", e);
			}
		}
		else {
			MCLogger.logError("Unrecognized URI " + uri);
		}
	}

}
