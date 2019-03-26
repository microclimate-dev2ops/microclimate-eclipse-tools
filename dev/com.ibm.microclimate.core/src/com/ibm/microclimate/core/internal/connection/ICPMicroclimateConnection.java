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
	
	public ICPMicroclimateConnection(URI uri, String masterIP) throws IOException, URISyntaxException, JSONException {
		super(uri);
		this.masterIP = masterIP;
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
	public IPath getLocalAppPath(MicroclimateApplication app) throws Exception {
		String localPath = ICPSyncManager.setupLocalProject(app);
		return new Path(localPath);
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
