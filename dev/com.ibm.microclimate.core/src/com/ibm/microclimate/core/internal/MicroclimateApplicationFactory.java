package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.core.internal.constants.ProjectType;

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
					String typeStr = appJso.getString(MCConstants.KEY_PROJECT_TYPE);
					type = ProjectType.fromInternalType(typeStr);
				}
				catch(JSONException e) {
					// Sometimes (seems to be when project is disabled) this value is missing -
					// see https://github.ibm.com/dev-ex/portal/issues/425
					MCLogger.logError(e.getMessage() + " in: " + appJso); //$NON-NLS-1$
				}

				String loc = appJso.getString(MCConstants.KEY_LOC_DISK);

				int httpPort = -1;

				// Is the app started?
				// If so, get the port. If not, leave port set to -1, this indicates the app is not started.
				if (appJso.has(MCConstants.KEY_APP_STATUS)
						&& AppState.STARTED.appState.equals(appJso.getString(MCConstants.KEY_APP_STATUS))) {

					try {
						String httpPortStr = appJso.getJSONObject(MCConstants.KEY_PORTS)
								.getString(MCConstants.KEY_EXPOSED_PORT);
						httpPort = Integer.parseInt(httpPortStr);
					}
					catch(JSONException e) {
						// Indicates the app is not started
						MCLogger.log(name + " has not bound to a port"); //$NON-NLS-1$
					}
					catch(NumberFormatException e) {
						MCLogger.logError("Error parsing port from " + appJso, e); //$NON-NLS-1$
					}
				}
				else {
					MCLogger.log(name + " is not running"); //$NON-NLS-1$
				}

				String contextRoot = null;
				if(appJso.has(MCConstants.KEY_CONTEXTROOT)) {
					contextRoot = appJso.getString(MCConstants.KEY_CONTEXTROOT);
				}

				String buildLogPath = null;
				boolean hasAppLog = false;
				if (appJso.has(MCConstants.KEY_LOGS)) {
					JSONObject logsJso = appJso.getJSONObject(MCConstants.KEY_LOGS);
					if (logsJso.has(MCConstants.KEY_LOG_BUILD)) {
						buildLogPath = logsJso.getJSONObject(MCConstants.KEY_LOG_BUILD)
								.getString(MCConstants.KEY_LOG_FILE);
					}

					if (logsJso.has(MCConstants.KEY_LOG_APP)) {
						hasAppLog = true;
					}
				}

				MicroclimateApplication mcApp = new MicroclimateApplication(mcConnection, id, name, type, loc,
						httpPort, contextRoot, buildLogPath, hasAppLog);
				apps.add(mcApp);
			}
			catch(JSONException e) {
				MCLogger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
			}
		}

		return apps;
	}
}
