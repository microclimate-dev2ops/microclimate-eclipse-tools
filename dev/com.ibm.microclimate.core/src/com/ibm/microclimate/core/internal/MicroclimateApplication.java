/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.console.ProjectLogInfo;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.BuildStatus;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectCapabilities;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.constants.StartMode;

/**
 * Represents a Microclimate Application / Project
 */
public class MicroclimateApplication {

	public final MicroclimateConnection mcConnection;
	public final String projectID, name, host;
	public final IPath fullLocalPath;
	public final ProjectType projectType;

	private String contextRoot;	// can be null
	private StartMode startMode;
	private AppState appState;
	private BuildStatus buildStatus;
	private String buildDetails;
	private boolean autoBuild = true;
	private boolean enabled = true;
	private String containerId;
	private ProjectCapabilities projectCapabilities;
	private String action;
	private List<ProjectLogInfo> logInfos = Collections.emptyList();
	private boolean metricsAvailable = false;

	// Must be updated whenever httpPort changes. Can be null
	private URL baseUrl;

	// These are set by the MicroclimateSocket so we have to make sure the reads and writes are synchronized
	// An httpPort of -1 indicates the app is not started - could be building or disabled.
	private int httpPort = -1, debugPort = -1;

	MicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, ProjectType projectType, String pathInWorkspace)
					throws MalformedURLException {

		this.mcConnection = mcConnection;
		this.projectID = id;
		this.name = name;
		this.projectType = projectType;
		this.host = mcConnection.baseUrl.getHost();

		// The mcConnection.localWorkspacePath will end in /microclimate-workspace
		// and the path passed here will start with /microclimate-workspace, so here we fix the duplication.
		this.fullLocalPath = MCUtil.appendPathWithoutDupe(mcConnection.getWorkspacePath(), pathInWorkspace);

		this.startMode = StartMode.RUN;
		this.appState = AppState.UNKNOWN;
		this.buildStatus = BuildStatus.UNKOWN;
	}

	private void setBaseUrl() throws MalformedURLException {
		if (httpPort == -1) {
			MCLogger.log("Un-setting baseUrl because httpPort is not valid"); //$NON-NLS-1$
			baseUrl = null;
			return;
		}

		baseUrl = new URL("http", host, httpPort, ""); //$NON-NLS-1$ //$NON-NLS-2$

		if (contextRoot != null && !contextRoot.isEmpty()) {
			baseUrl = new URL(baseUrl, contextRoot);
		}
	}
	
	public synchronized void setAppStatus(String appStatus) {
		this.appState = AppState.get(appStatus);
	}
	
	public synchronized void setBuildStatus(String buildStatus, String buildDetails) {
		if (buildStatus != null) {
			BuildStatus newStatus = BuildStatus.get(buildStatus);
			boolean hasChanged = newStatus != this.buildStatus;
			this.buildStatus = newStatus;
			if (buildDetails != null && buildDetails.trim().isEmpty()) {
				this.buildDetails = null;
			} else {
				this.buildDetails = buildDetails;
			}
			if (hasChanged && newStatus.isComplete()) {
				buildComplete();
			}
		}
	}
	
	public synchronized void setContextRoot(String contextRoot) {
		this.contextRoot = contextRoot;
		try {
			setBaseUrl();
		} catch (MalformedURLException e) {
			MCLogger.logError("An error occurred updating the base url with the new context root: " + contextRoot, e);
		}
	}
	
	public synchronized void setStartMode(StartMode startMode) {
		this.startMode = startMode;
	}
	
	public synchronized void setAutoBuild(boolean enabled) {
		this.autoBuild = enabled;
	}
	
	public synchronized void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public synchronized void setContainerId(String id) {
		this.containerId = id;
	}
	
	public synchronized void setAction(String action) {
		this.action = action;
	}
	
	public synchronized void setLogInfos(List<ProjectLogInfo> logInfos) {
		this.logInfos = logInfos;
	}
	
	public synchronized void setMetricsAvailable(boolean value) {
		metricsAvailable = value;
	}
	
	/**
	 * Can return null if this project hasn't started yet (ie httpPort == -1)
	 */
	public URL getBaseUrl() {
		return baseUrl;
	}
	
	public synchronized AppState getAppState() {
		return appState;
	}
	
	public synchronized BuildStatus getBuildStatus() {
		return buildStatus;
	}
	
	public synchronized String getBuildDetails() {
		return buildDetails;
	}

	public synchronized int getHttpPort() {
		return httpPort;
	}

	public synchronized int getDebugPort() {
		return debugPort;
	}

	public synchronized StartMode getStartMode() {
		return startMode;
	}
	
	public synchronized boolean isAutoBuild() {
		return autoBuild;
	}
	
	public synchronized boolean isEnabled() {
		return enabled;
	}
	
	public synchronized String getContainerId() {
		return containerId;
	}
	
	public boolean isActive() {
		return getAppState() == AppState.STARTING || getAppState() == AppState.STARTED;
	}

	public boolean isRunning() {
		return baseUrl != null;
	}
	
	public boolean isDeleting() {
		return MCConstants.VALUE_ACTION_DELETING.equals(action);
	}
	
	public boolean isImporting() {
		// The action value is called "validating" but really this means the project is importing
		return MCConstants.VALUE_ACTION_VALIDATING.equals(action);
	}
	
	public boolean isAvailable() {
		return isEnabled() && !isImporting();
	}
	
	public List<ProjectLogInfo> getLogInfos() {
		return logInfos;
	}

	public boolean hasBuildLog() {
		return (!projectType.isType(ProjectType.TYPE_NODEJS));
	}
	
	public synchronized boolean getMetricsAvailable() {
		return metricsAvailable;
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
	 * This is done because the application will wait for the ports to be
	 * set to something other than -1 before trying to connect.
	 */
	public synchronized void invalidatePorts() {
		MCLogger.log("Invalidate ports for " + name); //$NON-NLS-1$
		httpPort = -1;
		debugPort = -1;
	}

	/**
	 * Get the capabilities of a project.  Cache them because they should not change
	 * and since they are used to decide which menu items are shown/enabled this method
	 * needs to be fast.
	 */
	public ProjectCapabilities getProjectCapabilities() {
		if (projectCapabilities == null) {
			try {
				JSONObject obj = mcConnection.requestProjectCapabilities(this);
				projectCapabilities = new ProjectCapabilities(obj);
			} catch (Exception e) {
				MCLogger.logError("Failed to get the project capabilities for application: " + name, e); //$NON-NLS-1$
			}
		}
		if (projectCapabilities == null) {
			return ProjectCapabilities.emptyCapabilities;
		}
		return projectCapabilities;
	}
	
	public void clearDebugger() {
		// Override as needed
	}
	
	public void connectDebugger() {
		// Override as needed
	}
	
	public void reconnectDebugger() {
		// override as needed
	}

	public void dispose() {
		// Override as needed
	}
	
	public void resetValidation() {
		// Override as needed
	}
	
	public void validationError(String filePath, String message, String quickFixId, String quickFixDescription) {
		// Override as needed
	}
	
	public void validationWarning(String filePath, String message, String quickFixId, String quickFixDescription) {
		// Override as needed
	}
	
	public boolean supportsDebug() {
		// Override as needed
		return false;
	}
	
	public void buildComplete() {
		// Override to perform actions when a build has completed
	}

	@Override
	public String toString() {
		return String.format("%s@%s id=%s name=%s type=%s loc=%s", //$NON-NLS-1$
				MicroclimateApplication.class.getSimpleName(), baseUrl.toString(),
				projectID, name, projectType, fullLocalPath.toOSString());
	}
}

