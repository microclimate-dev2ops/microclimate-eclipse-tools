	package com.ibm.microclimate.core.server;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.ibm.microclimate.core.Activator;
import com.ibm.microclimate.core.MCLogger;

/**
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateServer extends ServerDelegate implements IURLProvider {

	public static final String SERVER_ID = "microclimate.server";		// must match ID in plugin.xml

	// Attributes
	public static final String
			ATTR_HTTP_PORT 	= "httpPort",
			// Root URL of this particular project
			ATTR_APP_URL  	= "appRootUrl",
			ATTR_PROJ_ID	= "projectID",
			// Base URL of the corresponding MicroclimateConnection
			ATTR_MCC_URL	= "mcBaseUrl",
			// The name of the Eclipse project associated with this server
			ATTR_ECLIPSE_PROJECT_NAME = "eclipseProjectName";

	private MicroclimateServerBehaviour behaviour;

	@Override
	public void setDefaults(IProgressMonitor monitor) {
		MCLogger.log("Initialize MicroclimateServer");

		behaviour = getServer().getAdapter(MicroclimateServerBehaviour.class);
	}

	@Override
	public ServerPort[] getServerPorts() {
		int httpPortNum = behaviour.getApp().getHttpPort();
		ServerPort httpPort = new ServerPort("microclimateServerPort", "httpPort", httpPortNum, "http");

		ServerPort debugPort = null;

		int debugPortNum = behaviour.getApp().getDebugPort();
		if (debugPortNum != -1) {
			debugPort = new ServerPort("microclimateServerPort", "debugPort", debugPortNum, "http");
		}

		if (debugPort != null) {
			return new ServerPort[] { httpPort, debugPort };
		}
		else {
			return new ServerPort[] { httpPort };
		}
	}

	@Override
	public URL getModuleRootURL(IModule arg0) {
		String rootUrl = getServer().getAttribute(ATTR_APP_URL, "");
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
