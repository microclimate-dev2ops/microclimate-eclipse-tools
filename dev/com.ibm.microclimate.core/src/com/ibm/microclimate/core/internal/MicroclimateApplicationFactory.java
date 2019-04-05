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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.console.ProjectLogInfo;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.constants.StartMode;

public class MicroclimateApplicationFactory {
	
	/**
	 * Process the json for all projects, create or update applications as needed.
	 */
	public static void getAppsFromProjectsJson(MicroclimateConnection mcConnection, String projectsJson) {
		getAppsFromProjectsJson(mcConnection, projectsJson, null);
	}
	
	/**
	 * Process the json for the given projectID or all projects if projectID is null.
	 */
	public static void getAppsFromProjectsJson(MicroclimateConnection mcConnection,
			String projectsJson, String projectID) {

		try {
			MCLogger.log(projectsJson);
			JSONArray appArray = new JSONArray(projectsJson);
			Set<String> idSet = new HashSet<String>();
	
			for(int i = 0; i < appArray.length(); i++) {
				JSONObject appJso = appArray.getJSONObject(i);
				try {
					String id = appJso.getString(MCConstants.KEY_PROJECT_ID);
					idSet.add(id);
					// If a project id was passed in then only process the JSON object for that project
					if (projectID == null || projectID.equals(id)) {
						synchronized(MicroclimateApplicationFactory.class) {
							MicroclimateApplication app = mcConnection.getAppByID(id);
							if (app != null) {
								updateApp(app, appJso);
								if (app.isDeleting()) {
									// Remove the app from the list
									mcConnection.removeApp(id);
								}
							} else {
								app = createApp(mcConnection, appJso);
								if (app != null && !app.isDeleting()) {
									mcConnection.addApp(app);
								}
							}
						}
					}
				} catch (Exception e) {
					MCLogger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
				}
			}
			
			// If refreshing all of the projects, remove any projects that are not in the list returned by Microclimate.
			// This will only happen if something goes wrong and no delete event is received from Microclimate for a
			// project.
			if (projectID == null) {
				for (String id : mcConnection.getAppIds()) {
					if (!idSet.contains(id)) {
						mcConnection.removeApp(id);
					}
				}
			}
		} catch (Exception e) {
			MCLogger.logError("Error parsing json for project array.", e); //$NON-NLS-1$
		}
	}
	
	/**
	 * Use the static information in the JSON object to create the application.
	 */
	public static MicroclimateApplication createApp(MicroclimateConnection mcConnection, JSONObject appJso) {
		try {
			// MCLogger.log("app: " + appJso.toString());
			String name = appJso.getString(MCConstants.KEY_NAME);
			String id = appJso.getString(MCConstants.KEY_PROJECT_ID);

			ProjectType type = ProjectType.UNKNOWN_TYPE;
			try {
				// from portal: projectType and buildType are equivalent - however
				// buildType is always present, projectType is missing for disabled/stopped projects
				// We should use projectType if it gets fixed.
				// String typeStr = appJso.getString(MCConstants.KEY_PROJECT_TYPE);

				String typeStr = appJso.getString(MCConstants.KEY_BUILD_TYPE);
				String languageStr = appJso.getString(MCConstants.KEY_LANGUAGE);
				type = new ProjectType(typeStr, languageStr);
			}
			catch(JSONException e) {
				MCLogger.logError(e.getMessage() + " in: " + appJso); //$NON-NLS-1$
			}

			String loc = appJso.getString(MCConstants.KEY_LOC_DISK);
			
			MicroclimateApplication mcApp = MicroclimateObjectFactory.createMicroclimateApplication(mcConnection, id, name, type, loc);
			
			updateApp(mcApp, appJso);
			return mcApp;
		} catch(JSONException e) {
			MCLogger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
		} catch (Exception e) {
			MCLogger.logError("Error creating new application for project.", e); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * Update the application with the dynamic information in the JSON object.
	 */
	public static void updateApp(MicroclimateApplication mcApp, JSONObject appJso) {
		try {
			// Set the action
			if (appJso.has(MCConstants.KEY_ACTION)) {
				String action = appJso.getString(MCConstants.KEY_ACTION);
				mcApp.setAction(action);
				if (MCConstants.VALUE_ACTION_DELETING.equals(action)) {
					// No point in updating any further since this app should be removed from the list
					return;
				}
			} else {
				mcApp.setAction(null);
			}
			
			// Set the app status
			if (appJso.has(MCConstants.KEY_APP_STATUS)) {
				String appStatus = appJso.getString(MCConstants.KEY_APP_STATUS);
				if (appStatus != null) {
					mcApp.setAppStatus(appStatus);
				}
			}
			
			// Set the build status
			if (appJso.has(MCConstants.KEY_BUILD_STATUS)) {
				String buildStatus = appJso.getString(MCConstants.KEY_BUILD_STATUS);
				String detail = ""; //$NON-NLS-1$
				if (appJso.has(MCConstants.KEY_DETAILED_BUILD_STATUS)) {
					detail = appJso.getString(MCConstants.KEY_DETAILED_BUILD_STATUS);
				}
				mcApp.setBuildStatus(buildStatus, detail);
			}
			
			// Get the container id
			String containerId = null;
			if (appJso.has(MCConstants.KEY_CONTAINER_ID)) {
			    containerId = appJso.getString(MCConstants.KEY_CONTAINER_ID);
			}
			mcApp.setContainerId(containerId);
			
			// Get the ports if they are available
			try {
				if (appJso.has(MCConstants.KEY_PORTS) && (appJso.get(MCConstants.KEY_PORTS) instanceof JSONObject)) {
					JSONObject portsObj = appJso.getJSONObject(MCConstants.KEY_PORTS);
	
					int httpPortNum = -1;
					if (portsObj != null && portsObj.has(MCConstants.KEY_EXPOSED_PORT)) {
						String httpPort = portsObj.getString(MCConstants.KEY_EXPOSED_PORT);
						if (httpPort != null && !httpPort.isEmpty()) {
							httpPortNum = MCUtil.parsePort(httpPort);
						}
					}
					if (httpPortNum != -1) {
						mcApp.setHttpPort(httpPortNum);
					}
	
					int debugPortNum = -1;
					if (portsObj != null && portsObj.has(MCConstants.KEY_EXPOSED_DEBUG_PORT)) {
						String debugPort = portsObj.getString(MCConstants.KEY_EXPOSED_DEBUG_PORT);
						if (debugPort != null && !debugPort.isEmpty()) {
							debugPortNum = MCUtil.parsePort(debugPort);
						}
					}
					mcApp.setDebugPort(debugPortNum);

				} else {
					MCLogger.logError("No ports object on project info for application: " + mcApp.name); //$NON-NLS-1$
				}
			} catch (Exception e) {
				MCLogger.logError("Failed to get the ports for application: " + mcApp.name, e); //$NON-NLS-1$
			}
			
			// Set the context root
			String contextRoot = null;
			if (appJso.has(MCConstants.KEY_CONTEXTROOT)) {
				contextRoot = appJso.getString(MCConstants.KEY_CONTEXTROOT);
			} else if (appJso.has(MCConstants.KEY_CUSTOM)) {
				JSONObject custom = appJso.getJSONObject(MCConstants.KEY_CUSTOM);
				if (custom.has(MCConstants.KEY_CONTEXTROOT)) {
					contextRoot = custom.getString(MCConstants.KEY_CONTEXTROOT);
				}
			}
			mcApp.setContextRoot(contextRoot);
			
			// Set the start mode
			StartMode startMode = StartMode.get(appJso);
			mcApp.setStartMode(startMode);
			
			// Set auto build
			if (appJso.has(MCConstants.KEY_AUTO_BUILD)) {
				boolean autoBuild = appJso.getBoolean(MCConstants.KEY_AUTO_BUILD);
				mcApp.setAutoBuild(autoBuild);
			}
		} catch(JSONException e) {
			MCLogger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
		}
		
		try {
			// Set the log information
			List<ProjectLogInfo> logInfos = mcApp.mcConnection.requestProjectLogs(mcApp);
			mcApp.setLogInfos(logInfos);
		} catch (Exception e) {
			MCLogger.logError("An error occurred while updating the log information for project: " + mcApp.name, e);
		}
		
		// Check for metrics support
		boolean metricsAvailable = true;
		try {
			JSONObject obj = mcApp.mcConnection.requestProjectMetricsStatus(mcApp);
			if (obj != null && obj.has(MCConstants.KEY_METRICS_AVAILABLE)) {
				metricsAvailable = obj.getBoolean(MCConstants.KEY_METRICS_AVAILABLE);
			}
		} catch (Exception e) {
			MCLogger.logError("An error occurred checking if metrics are available: " + mcApp.name, e);
		}
		mcApp.setMetricsAvailable(metricsAvailable);
		
	}
}
