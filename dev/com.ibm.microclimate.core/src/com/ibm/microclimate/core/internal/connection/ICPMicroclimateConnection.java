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
import org.eclipse.equinox.security.storage.StorageException;
import org.json.JSONException;

import com.ibm.microclimate.core.internal.HttpUtil;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.auth.Authenticator;

import io.socket.client.IO;
import io.socket.client.Socket;
import okhttp3.OkHttpClient;

public class ICPMicroclimateConnection extends MicroclimateConnection {
	
	private final String masterIP;
	private final String namespace;
	
	public ICPMicroclimateConnection(URI uri, String masterIP, String namespace) throws IOException, URISyntaxException, JSONException {
		super(uri);
		this.masterIP = masterIP;
		this.namespace = namespace;
	}
	
	public String getMasterIP() {
		return masterIP;
	}
	
	public String getNamespace() {
		return namespace;
	}
	
	@Override
	public MicroclimateApplication removeApp(String projectID) {
		MicroclimateApplication app = super.removeApp(projectID);
		if (app != null) {
			try {
				ICPSyncManager.removeLocalProject(app);
			} catch (Exception e) {
				MCLogger.logError("An error occurred trying to stop synchronization for application: " + app, e);
			}
		}
		return app;
	}

	@Override
	public void close() {
		super.close();
		try {
			ICPSyncManager.removeProjectsForConnection(this);
		} catch (Exception e) {
			MCLogger.logError("An error occurred while removing projects for connection: " + baseUrl, e);
		}
	}

	@Override
	public String getAuthToken() throws IOException {
		try {
			return Authenticator.instance().getToken(masterIP);
		} catch (StorageException e) {
			String msg = "Failed to get the authorization token for: " + masterIP;
			MCLogger.logError(msg, e);
			throw new IOException(msg, e);
		}
	}

	@Override
	public String getLocalAppPath(MicroclimateApplication app, IProgressMonitor monitor) throws Exception {
		return ICPSyncManager.setupLocalProject(this, app, monitor);
	}
	
	@Override
	public ConnectionType getType() {
		return ConnectionType.ICP_CONNECTION;
	}

	@Override
	public Socket getSocket() throws IOException {
		
		OkHttpClient okHttpClient = new OkHttpClient.Builder()
				  .sslSocketFactory(HttpUtil.getSocketFactory(), HttpUtil.getTrustManager())
				  .build();
		
		URI uri = baseUrl;
		if (getSocketNamespace() != null) {
			uri = uri.resolve(getSocketNamespace());
		}
		socketURI = uri;
		
		IO.Options opts = new IO.Options();
		opts.callFactory = okHttpClient;
		opts.webSocketFactory = okHttpClient;
		
		return IO.socket(socketURI, opts);
	}
}
