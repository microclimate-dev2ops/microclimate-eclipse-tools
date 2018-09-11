/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import java.net.ConnectException;
import java.net.URI;

import org.eclipse.osgi.util.NLS;

import com.ibm.microclimate.core.internal.Messages;

/**
 * Custom exception to indicate that connecting to the Microclimate Socket at the given URL failed.
 *
 * @author timetchells@ibm.com
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
