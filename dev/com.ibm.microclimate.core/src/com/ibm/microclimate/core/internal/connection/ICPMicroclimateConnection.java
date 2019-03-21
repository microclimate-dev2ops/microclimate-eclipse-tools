package com.ibm.microclimate.core.internal.connection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONException;

import com.ibm.microclimate.core.internal.MicroclimateApplication;

public class ICPMicroclimateConnection extends MicroclimateConnection {
	
	public ICPMicroclimateConnection(URI uri) throws IOException, URISyntaxException, JSONException {
		super(uri);
	}

	@Override
	public IPath getLocalAppPath(MicroclimateApplication app) throws Exception {
		String localPath = ICPSyncManager.setupLocalProject(app);
		return new Path(localPath);
	}
	
}
