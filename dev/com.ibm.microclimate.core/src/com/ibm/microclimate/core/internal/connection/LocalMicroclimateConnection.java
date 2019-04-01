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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.messages.Messages;

import io.socket.client.IO;
import io.socket.client.Socket;

public class LocalMicroclimateConnection extends MicroclimateConnection {
	
	private IPath localWorkspacePath;
	
	public LocalMicroclimateConnection(URI uri) throws IOException, URISyntaxException, JSONException {
		super(uri);
	}

	@Override
	protected void initialize(JSONObject env) throws IOException, URISyntaxException, JSONException {
		super.initialize(env);

		this.localWorkspacePath = getWorkspacePath(env);
		if (localWorkspacePath == null) {
			// Can't recover from this
			// This should never happen since we have already determined it is a supported version of microclimate.
			onInitFail(Messages.MicroclimateConnection_ErrConnection_WorkspaceErr);
		}
	}
	
	@Override
	public synchronized boolean clearConnectionError(JSONObject envData) throws IOException, JSONException, URISyntaxException {
		if (!super.clearConnectionError(envData)) {
			return false;
		}
		
		IPath path = getWorkspacePath(envData);
		if (path == null) {
			// This should not happen since the version was ok
			MCLogger.logError("Failed to get the local workspace path after reconnect");
			this.connectionErrorMsg = Messages.MicroclimateConnection_ErrConnection_WorkspaceErr;
			MCUtil.updateConnection(this);
			return false;
		}
		this.localWorkspacePath = path;
		return true;
	}

	@Override
	public String getLocalAppPath(MicroclimateApplication app, IProgressMonitor monitor) {
		return MCUtil.appendPathWithoutDupe(localWorkspacePath, app.pathInWorkspace);
	}

	@Override
	public ConnectionType getType() {
		return ConnectionType.LOCAL_CONNECTION;
	}
	
	@Override
	public Socket getSocket() {
		URI uri = baseUrl;
		if (getSocketNamespace() != null) {
			uri = uri.resolve(getSocketNamespace());
		}

		socketURI = uri;
		return IO.socket(socketURI);
	}

	public IPath getWorkspacePath() {
		return localWorkspacePath;
	}
	
	private static Path getWorkspacePath(JSONObject env) throws JSONException {
		// Try the internal system property first
		String path = System.getProperty(MICROCLIMATE_WORKSPACE_PROPERTY, null);
		if (path != null && !path.isEmpty()) {
			return new Path(path);
		}

		if (!env.has(MCConstants.KEY_ENV_WORKSPACE_LOC)) {
			MCLogger.logError("Missing workspace location from env data"); //$NON-NLS-1$
			return null;
		}
		String workspaceLoc = env.getString(MCConstants.KEY_ENV_WORKSPACE_LOC);
		if (MCUtil.isWindows() && workspaceLoc.startsWith("/")) { //$NON-NLS-1$
			String device = workspaceLoc.substring(1, 2);
			workspaceLoc = device + ":" + workspaceLoc.substring(2); //$NON-NLS-1$
		}
		return new Path(workspaceLoc);
	}
	
	@Override
	public String toString() {
		return String.format("%s @ baseUrl=%s workspacePath=%s numApps=%d", //$NON-NLS-1$
				MicroclimateConnection.class.getSimpleName(), baseUrl, localWorkspacePath, appMap.size());
	}

	

}
