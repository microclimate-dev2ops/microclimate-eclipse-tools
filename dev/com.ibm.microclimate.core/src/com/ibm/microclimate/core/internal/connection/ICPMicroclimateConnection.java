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

package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONException;

import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection.ConnectionType;

public class ICPMicroclimateConnection extends MicroclimateConnection {
	
	public ICPMicroclimateConnection(URI uri) throws IOException, URISyntaxException, JSONException {
		super(uri);
	}

	@Override
	public IPath getLocalAppPath(MicroclimateApplication app) throws Exception {
		String localPath = ICPSyncManager.setupLocalProject(app);
		return new Path(localPath);
	}
	
	@Override
	public ConnectionType getType() {
		return ConnectionType.ICP_CONNECTION;
	}

	
}
