package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MCLogger;

public class MicroclimateApplication {

	public final MicroclimateConnection mcConnection;
	public final String projectID, name, language, host, contextRoot;
	public final IPath fullLocalPath;
	public final URL rootUrl;

	private String appStatus;
	private String buildStatus;
	private String buildStatusDetail;
	private int httpPort = -1, debugPort = -1;

	MicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, String language, String pathWithinWorkspace,
			int httpPort, String contextRoot)

					throws MalformedURLException {

		this.mcConnection = mcConnection;
		this.projectID = id;
		this.name = name;
		this.language = language;
		this.fullLocalPath = mcConnection.localWorkspacePath.append(pathWithinWorkspace);
		this.httpPort = httpPort;
		this.contextRoot = contextRoot;
		this.host = mcConnection.host;

		URL rootUrl = new URL("http", host, httpPort, "");

		if (contextRoot != null) {
			rootUrl = new URL(rootUrl, contextRoot);
		}

		this.rootUrl = rootUrl;

		//MCLogger.log("Created mcApp:");
		//MCLogger.log(toString());
	}

	public static List<MicroclimateApplication> buildFromProjectsJson(MicroclimateConnection conn,
			String projectsJson) throws JSONException, NumberFormatException, MalformedURLException {

		List<MicroclimateApplication> result = new ArrayList<>();

		JSONArray appArray = new JSONArray(projectsJson);

		List<String> appsStillStarting = new ArrayList<>();
		for(int i = 0; i < appArray.length(); i++) {
			JSONObject app = appArray.getJSONObject(i);
			try {
				MCLogger.log("app: " + app.toString());
				String id 	= app.getString("projectID");
				String name = app.getString("name");
				String lang = app.getString("language");
				String loc 	= app.getString("locOnDisk");

				String exposedPort = "";

				// Often this step fails because "ports is not a json object". This means that the application is still
				// starting up, so MC doesn't know its ports yet. Waiting a few seconds and then refreshing
				// the project list works around this. See the catch block for how this is communicated to the user.
				try {
					exposedPort = app.getJSONObject("ports").getString("exposedPort");
				}
				catch(JSONException e) {
					if (e.getMessage().equals("JSONObject[\"ports\"] is not a JSONObject.")) {
						appsStillStarting.add(name);
					}
					continue;
				}

				int httpPort = Integer.parseInt(exposedPort);

				final String contextRootKey = "contextroot";
				String contextRoot = null;
				if(app.has(contextRootKey)) {
					contextRoot = app.getString(contextRootKey);
				}

				result.add(new MicroclimateApplication(conn, id, name, lang, loc, httpPort, contextRoot));

			}
			catch(JSONException e) {
				MCLogger.logError("Error parsing project json: " + app, e);
			}
		}

		displayAppsStartingMsg(appsStillStarting);

		return result;
	}

	private static void displayAppsStartingMsg(List<String> appsStarting) {
		StringBuilder startingAppsBuilder = new StringBuilder();

		if (appsStarting.size() == 0) {
			return;
		}
		else if (appsStarting.size() == 1) {
			startingAppsBuilder.append(appsStarting.get(0));
		}
		else {
			for (int i = 0; i < appsStarting.size(); i++) {
				String appName = appsStarting.get(i);

				if (appsStarting.size() > 1) {
					if (i == appsStarting.size() - 1) {
						// Final item
						startingAppsBuilder.append("and ").append(appName);
					}
					else {
						startingAppsBuilder.append(appName).append(", ");
					}
				}
			}
		}

		String appsStartingMsg = "The following application(s) exist but are still starting up: " +
				startingAppsBuilder.toString() +
				"\nPlease wait a few seconds and then refresh the projects list " +
				"if you wish to link one of these applications.";

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Applications still starting",
						appsStartingMsg);
			}
		});
	}

	// Getters for our project state fields

	public synchronized String getAppStatus() {
		return appStatus;
	}

	public synchronized String getBuildStatus() {
		return buildStatus;
	}

	public synchronized String getBuildStatusDetail() {
		return buildStatusDetail;
	}

	public synchronized int getHttpPort() {
		return httpPort;
	}

	public synchronized int getDebugPort() {
		return debugPort;
	}

	// Setters to be called by the MCSocket to update this project's state

	public synchronized void setAppStatus(String appStatus) {
		this.appStatus = appStatus;
	}

	public synchronized void setBuildStatusDetail(String buildStatusDetail) {
		this.buildStatusDetail = buildStatusDetail;
	}

	public synchronized void setBuildStatus(String buildStatus) {
		this.buildStatus = buildStatus;
	}

	public synchronized void setHttpPort(int httpPort) {
		MCLogger.log("Set HTTP port for " + rootUrl.toString() + " to " + httpPort);
		this.httpPort = httpPort;
	}

	public synchronized void setDebugPort(int debugPort) {
		MCLogger.log("Set debug port for " + rootUrl.toString() + " to " + debugPort);
		this.debugPort = debugPort;
	}

	/**
	 * Invalidate fields that can change when the application is restarted.
	 * On restart success, these will be updated by the Socket handler for that event.
	 */
	public synchronized void invalidatePorts() {
		MCLogger.log("Invalidate ports for " + rootUrl.toString());
		httpPort = -1;
		debugPort = -1;
	}

	@Override
	public String toString() {
		return String.format("%s@%s id=%s name=%s language=%s loc=%s",
				MicroclimateApplication.class.getSimpleName(), rootUrl.toString(),
				projectID, name, language, fullLocalPath.toOSString());
	}
}
