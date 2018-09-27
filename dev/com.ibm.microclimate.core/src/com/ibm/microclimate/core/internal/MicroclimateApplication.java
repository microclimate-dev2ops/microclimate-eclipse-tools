/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.server.MicroclimateServer;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;

/**
 * Data type class to represent a Microclimate Application / Project
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateApplication {

	public final MicroclimateConnection mcConnection;
	public final String projectID, name, projectType, host;
	public final String contextRoot;	// can be null
	public final IPath fullLocalPath;

	public final String buildLogPath;	// can, but shouldn't, be null
	public final boolean hasAppLog;

	// Must be updated whenever httpPort changes. Can be null
	private URL baseUrl;

	// These are set by the MicroclimateSocket so we have to make sure the reads and writes are synchronized
	// An httpPort of -1 indicates the app is not started - could be building or disabled.
	private int httpPort = -1, debugPort = -1;

	MicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, String projectType, String pathInWorkspace,
			int httpPort, String contextRoot, String buildLogPath, boolean hasAppLog)
					throws MalformedURLException {

		this.mcConnection = mcConnection;
		this.projectID = id;
		this.name = name;
		this.projectType = projectType;
		this.httpPort = httpPort;
		this.contextRoot = contextRoot;
		this.host = mcConnection.baseUrl.getHost();
		this.buildLogPath = buildLogPath;
		this.hasAppLog = hasAppLog;

		// The mcConnection.localWorkspacePath will end in /microclimate-workspace
		// and the path passed here will start with /microclimate-workspace, so here we fix the duplication.
		this.fullLocalPath = MCUtil.appendPathWithoutDupe(mcConnection.localWorkspacePath, pathInWorkspace);

		setBaseUrl();

		//MCLogger.log("Created mcApp:");
		//MCLogger.log(toString());
	}

	public static MicroclimateServerBehaviour getServerWithProjectID(String projectID) {
		for (IServer server : ServerCore.getServers()) {
			if (projectID.equals(server.getAttribute(MicroclimateServer.ATTR_PROJ_ID, ""))) { //$NON-NLS-1$
				return server.getAdapter(MicroclimateServerBehaviour.class);
			}
		}
		return null;
	}

	private void setBaseUrl() throws MalformedURLException {
		if (httpPort == -1) {
			MCLogger.log("Un-setting baseUrl because httpPort is not valid"); //$NON-NLS-1$
			baseUrl = null;
			return;
		}

		baseUrl = new URL("http", host, httpPort, ""); //$NON-NLS-1$ //$NON-NLS-2$

		if (contextRoot != null) {
			baseUrl = new URL(baseUrl, contextRoot);
		}
	}

	// Getters for our project state fields

	/**
	 * Can return null if this project hasn't started yet (ie httpPort == -1)
	 */
	public URL getBaseUrl() {
		return baseUrl;
	}

	public synchronized int getHttpPort() {
		return httpPort;
	}

	public synchronized int getDebugPort() {
		return debugPort;
	}

	public boolean isLinked() {
		return getLinkedServer() != null;
	}

	public MicroclimateServerBehaviour getLinkedServer() {
		// Before, we kept a reference to the linked IServer, which was set by the ServerBehaviour on initialize,
		// and unset on dispose. This new approach removes bugs associated with errors in that state,
		// but means re-acquiring the server each time it is requested, which is not efficient.
		return getServerWithProjectID(projectID);
	}

	public boolean isLinkable() {
		return isRunning() && isMicroprofileProject() && !isLinked();
	}

	public boolean isRunning() {
		return baseUrl != null;
	}

	public boolean isMicroprofileProject() {
		return MCConstants.PROJECT_TYPE_LIBERTY.equals(projectType);
	}

	public String getUserFriendlyType() {
		return MCConstants.projectTypeToUserFriendly(projectType);
	}

	public synchronized void setHttpPort(int httpPort) {
		MCLogger.log("Set HTTP port for " + baseUrl + " to " + httpPort); //$NON-NLS-1$ //$NON-NLS-2$
		this.httpPort = httpPort;
		try {
			setBaseUrl();
		} catch (MalformedURLException e) {
			MCLogger.logError(e);
		}
	}

	public synchronized void setDebugPort(int debugPort) {
		MCLogger.log("Set debug port for " + baseUrl + " to " + debugPort); //$NON-NLS-1$ //$NON-NLS-2$
		this.debugPort = debugPort;
	}

	/**
	 * Invalidate fields that can change when the application is restarted.
	 * On restart success, these will be updated by the Socket handler for that event.
	 * This is done because the server will wait for the ports to be
	 * set to something other than -1 before trying to connect.
	 */
	public synchronized void invalidatePorts() {
		MCLogger.log("Invalidate ports for " + name); //$NON-NLS-1$
		httpPort = -1;
		debugPort = -1;
	}

	@Override
	public String toString() {
		return String.format("%s@%s id=%s name=%s type=%s loc=%s", //$NON-NLS-1$
				MicroclimateApplication.class.getSimpleName(), baseUrl.toString(),
				projectID, name, projectType, fullLocalPath.toOSString());
	}
}

