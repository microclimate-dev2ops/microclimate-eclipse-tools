package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IPath;
import org.json.JSONException;

import com.ibm.microclimate.core.internal.MicroclimateApplication;

public class ICPMicroclimateConnection extends MicroclimateConnection {
	
	public ICPMicroclimateConnection(URI uri) throws IOException, URISyntaxException, JSONException {
		super(uri);
	}

	@Override
	public IPath getLocalAppPath(MicroclimateApplication app) {
		// TODO
		// Need synchronization util that will set up sync
		// for the remote path and return the local path
		// Save in preferences project name and local path
		return null;
	}
	
}
