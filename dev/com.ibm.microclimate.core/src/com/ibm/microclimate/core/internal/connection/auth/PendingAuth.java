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

import com.nimbusds.oauth2.sdk.id.State;

/**
 * Data type for storing the current pending authentication - 
 * to maintain state between the user launching the browser to log in, and Eclipse receiving the callback.
 */
public class PendingAuth {
	public final State state;
	public final String masterNodeIP;
	
	public PendingAuth(String masterNodeIP, State state) {
		this.masterNodeIP = masterNodeIP;
		this.state = state;
	}
	
	@Override
	public String toString() {
		return "PendingAuth for " + this.masterNodeIP;
	}
}
