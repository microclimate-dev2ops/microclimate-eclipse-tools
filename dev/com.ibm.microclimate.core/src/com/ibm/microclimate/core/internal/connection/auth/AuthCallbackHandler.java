/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

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
