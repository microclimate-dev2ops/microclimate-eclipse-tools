	package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.ibm.microclimate.core.Activator;
import com.ibm.microclimate.core.MCLogger;

public class MicroclimateServer extends ServerDelegate implements IURLProvider {

	public static final String SERVER_ID = "microclimate.server";		// must match ID in plugin.xml

	// Attributes
	public static final String
			ATTR_HTTP_PORT 	= "httpPort",
			ATTR_ROOT_URL  	= "rootUrl",
			ATTR_PROJ_ID   	= "projectID";

	//private MicroclimateServerBehaviour behaviour;

	@Override
	public void initialize() {
		MCLogger.log("Initialize MicroclimateServer");

		//behaviour = (MicroclimateServerBehaviour) getServer().loadAdapter(MicroclimateServerBehaviour.class, null);
	}

	@Override
	public ServerPort[] getServerPorts() {
		if(getServer().getServerState() != IServer.STATE_STARTED) {
			return new ServerPort[0];
		}

		// TODO cache this, TODO add debug port
		ServerPort[] serverPorts = new ServerPort[1];

		int httpPortNum = getServer().getAttribute(ATTR_HTTP_PORT, -1);
		if (httpPortNum == -1) {
			MCLogger.logError("No httpPort attribute");
		}
		else {
			serverPorts[0] = new ServerPort("microclimateServerPort", "httpPort", httpPortNum, "http");
		}

		return serverPorts;
	}

	@Override
	public URL getModuleRootURL(IModule arg0) {
		String rootUrl = getServer().getAttribute(ATTR_ROOT_URL, "");
		if(rootUrl.isEmpty()) {
			MCLogger.logError("No rootUrl attribute");
			return null;
		}

		URL url = null;
		try {
			url = new URL(rootUrl);
		} catch (MalformedURLException e) {
			MCLogger.logError(e);
		}

		return url;
	}

	@Override
	public IStatus canModifyModules(IModule[] arg0, IModule[] arg1) {
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Modules cannot be modified on a microclimate server", null);
	}

	@Override
	public IModule[] getChildModules(IModule[] arg0) {
		return null;
	}

	@Override
	public IModule[] getRootModules(IModule arg0) throws CoreException {
		return null;
	}

	@Override
	public void modifyModules(IModule[] arg0, IModule[] arg1, IProgressMonitor arg2) throws CoreException {
		// Do nothing - not supported
	}



}
