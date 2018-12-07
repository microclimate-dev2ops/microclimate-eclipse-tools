/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import java.net.ConnectException;
import java.net.URI;

import org.eclipse.osgi.util.NLS;

import com.ibm.microclimate.core.internal.messages.Messages;

/**
 * Custom exception to indicate that connecting to the Microclimate Socket at the given URL failed.
 */
public class MicroclimateConnectionException extends ConnectException {
	private static final long serialVersionUID = -7026779560626815421L;

	public final URI connectionUrl;

	public final String message;

	public MicroclimateConnectionException(URI url) {
		String msg = NLS.bind(Messages.MicroclimateConnectionException_ConnectingToMCFailed, url);

		message = msg;
		connectionUrl = url;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
