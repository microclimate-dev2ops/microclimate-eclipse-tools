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
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectType;
import com.ibm.microclimate.core.internal.constants.StartMode;

public class MicroclimateApplicationFactory {
	/**
	 * Parse the given projectsJson from the Microclimate server to construct a list of applications on that server.
	 */
	public static List<MicroclimateApplication> getAppsFromProjectsJson(MicroclimateConnection mcConnection,
			String projectsJson)
					throws JSONException, NumberFormatException, MalformedURLException {

		List<MicroclimateApplication> apps = new ArrayList<>();

		MCLogger.log(projectsJson);
		JSONArray appArray = new JSONArray(projectsJson);

		for(int i = 0; i < appArray.length(); i++) {
			JSONObject appJso = appArray.getJSONObject(i);
			try {
				// MCLogger.log("app: " + appJso.toString());
				String name = appJso.getString(MCConstants.KEY_NAME);
				String id = appJso.getString(MCConstants.KEY_PROJECT_ID);

				ProjectType type = ProjectType.UNKNOWN;
				try {
					// from portal: projectType and buildType are equivalent - however
					// buildType is always present, projectType is missing for disabled/stopped projects
					// We should use projectType if it gets fixed
					// see see https://github.ibm.com/dev-ex/portal/issues/520
					// String typeStr = appJso.getString(MCConstants.KEY_PROJECT_TYPE);

					String typeStr = appJso.getString(MCConstants.KEY_BUILD_TYPE);
					type = ProjectType.fromInternalType(typeStr);
				}
				catch(JSONException e) {
					MCLogger.logError(e.getMessage() + " in: " + appJso); //$NON-NLS-1$
				}

				String loc = appJso.getString(MCConstants.KEY_LOC_DISK);

				String contextRoot = null;
				if(appJso.has(MCConstants.KEY_CONTEXTROOT)) {
					contextRoot = appJso.getString(MCConstants.KEY_CONTEXTROOT);
				}

				MicroclimateApplication mcApp = new MicroclimateApplication(mcConnection, id, name, type, loc, contextRoot);
				
				// Set the initial app status
				if (appJso.has(MCConstants.KEY_APP_STATUS)) {
					String appStatus = appJso.getString(MCConstants.KEY_APP_STATUS);
					if (appStatus != null) {
						mcApp.setAppStatus(appStatus);
					}
				}
				
				// Get the ports if they are available
				try {
					if (appJso.has(MCConstants.KEY_PORTS)) {
						JSONObject portsObj = appJso.getJSONObject(MCConstants.KEY_PORTS);
	
						if (portsObj != null && portsObj.has(MCConstants.KEY_EXPOSED_PORT)) {
							String httpPort = portsObj.getString(MCConstants.KEY_EXPOSED_PORT);
							if (httpPort != null && !httpPort.isEmpty()) {
								int portNum = MCUtil.parsePort(httpPort);
								if (portNum != -1) {
									mcApp.setHttpPort(portNum);
								}
							}
						}
	
						if (portsObj != null && portsObj.has(MCConstants.KEY_EXPOSED_DEBUG_PORT)) {
							String debugPort = portsObj.getString(MCConstants.KEY_EXPOSED_DEBUG_PORT);
							if (debugPort != null && !debugPort.isEmpty()) {
								int portNum = MCUtil.parsePort(debugPort);
								if (portNum != -1) {
									mcApp.setDebugPort(portNum);
								}
							}
						}
					}
				} catch (Exception e) {
					MCLogger.logError("Failed to get the ports for application: " + name, e);
				}
				
				// Set the start mode
				StartMode startMode = StartMode.get(appJso);
				mcApp.setStartMode(startMode);
				
				apps.add(mcApp);
			}
			catch(JSONException e) {
				MCLogger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
			}
		}

		return apps;
	}
}
