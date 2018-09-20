/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

	package com.ibm.microclimate.core.internal.server;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.ServerPort;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.messages.Messages;

/**
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateServer extends ServerDelegate implements IURLProvider {

	public static final String SERVER_ID = "microclimate.server";		// must match ID in plugin.xml //$NON-NLS-1$

	// Attributes
	public static final String
			// Root URL of this particular project
			ATTR_PROJ_ID	= "projectID", //$NON-NLS-1$
			// Base URL of the corresponding MicroclimateConnection
			ATTR_MCC_URL	= "mcBaseUrl", //$NON-NLS-1$
			// The name of the Eclipse project associated with this server
			ATTR_ECLIPSE_PROJECT_NAME = "eclipseProjectName"; //$NON-NLS-1$

	private MicroclimateServerBehaviour behaviour;

	@Override
	public void setDefaults(IProgressMonitor monitor) {
		MCLogger.log("Initialize MicroclimateServer"); //$NON-NLS-1$

		behaviour = getServer().getAdapter(MicroclimateServerBehaviour.class);
	}

	@Override
	public ServerPort[] getServerPorts() {
		final int httpPortNum = behaviour.getApp().getHttpPort();
		if (httpPortNum == -1) {
			MCLogger.logError("No HttpPort set for server " + getServer().getName()); //$NON-NLS-1$
			return null;
		}

		ServerPort httpPort = new ServerPort("microclimateServerPort", "httpPort", httpPortNum, "http"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		ServerPort debugPort = null;

		final int debugPortNum = behaviour.getApp().getDebugPort();
		if (debugPortNum != -1) {
			debugPort = new ServerPort("microclimateServerPort", "debugPort", debugPortNum, "http"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		return behaviour.getApp().getBaseUrl();
	}

	@Override
	public IStatus canModifyModules(IModule[] arg0, IModule[] arg1) {
		return new Status(IStatus.ERROR, MicroclimateCorePlugin.PLUGIN_ID, 0, Messages.MicroclimateServer_CantModifyModules, null);
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

		/*
		 * TODO display a dialog when user clicks "Add and Remove..."
		 * rather than having it silently fail when trying to get the server's runtime with:
		 *
		 java.lang.NullPointerException
			at org.eclipse.wst.server.ui.internal.view.servers.ModuleSloshAction.perform(ModuleSloshAction.java:78)
			at org.eclipse.wst.server.ui.internal.view.servers.AbstractServerAction.run(AbstractServerAction.java:64)
		 */
	}
}
