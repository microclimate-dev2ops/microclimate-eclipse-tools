package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MCLogger;

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
	public final URL rootUrl;

	private final Set<IPath> logPaths;

	// The preference key that will be used to store whether this app is currently linked in the workspace
	private final String isLinkedPrefsKey;
	private boolean isLinked;

	// App Status fields - These are set by the MicroclimateSocket, and read by the MicroclimateServerMonitorThread
	// so we have to make sure the reads and writes are synchronized
	private String appStatus;
	private String buildStatus;
	private String buildStatusDetail;
	private int httpPort = -1, debugPort = -1;

	MicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, String projectType, String pathWithinWorkspace,
			int httpPort, String contextRoot)
					throws MalformedURLException {

		this.mcConnection = mcConnection;
		this.projectID = id;
		this.name = name;
		this.projectType = projectType;
		this.fullLocalPath = mcConnection.localWorkspacePath.append(pathWithinWorkspace);
		this.httpPort = httpPort;
		this.contextRoot = contextRoot;
		this.host = mcConnection.host;
		this.logPaths = new HashSet<>();

		URL rootUrl = new URL("http", host, httpPort, "");

		if (contextRoot != null) {
			rootUrl = new URL(rootUrl, contextRoot);
		}

		this.rootUrl = rootUrl;

		this.isLinkedPrefsKey = mcConnection.baseUrl + id;
		this.isLinked = com.ibm.microclimate.core.Activator.getDefault().getPreferenceStore()
				.getBoolean(isLinkedPrefsKey);

		//MCLogger.log("Created mcApp:");
		//MCLogger.log(toString());
	}

	/**
	 * Parse the given projectsJson from the Microclimate server to construct a list of applications on that server.
	 */
	public static List<MicroclimateApplication> getAppsFromProjectsJson(MicroclimateConnection mcConnection,
			String projectsJson) throws JSONException, NumberFormatException, MalformedURLException {

		List<MicroclimateApplication> runningApps = new ArrayList<>();

		MCLogger.log(projectsJson);
		JSONArray appArray = new JSONArray(projectsJson);

		List<String> appsStillStarting = new ArrayList<>();
		for(int i = 0; i < appArray.length(); i++) {
			JSONObject appJso = appArray.getJSONObject(i);
			try {
				MCLogger.log("app: " + appJso.toString());
				String name = appJso.getString("name");

				String status = appJso.getString("appStatus");
				if (!"started".equals(status)) {
					// see displayAppsStartingMsg method for how this is handled
					appsStillStarting.add(name);
					continue;
				}

				String id 	= appJso.getString("projectID");
				String type = appJso.getString("projectType");
				String loc 	= appJso.getString("locOnDisk");

				String httpPortStr = appJso.getJSONObject("ports").getString("exposedPort");

				int httpPort = Integer.parseInt(httpPortStr);

				final String contextRootKey = "contextroot";
				String contextRoot = null;
				if(appJso.has(contextRootKey)) {
					contextRoot = appJso.getString(contextRootKey);
				}

				MicroclimateApplication mcApp =
						new MicroclimateApplication(mcConnection, id, name, type, loc, httpPort, contextRoot);
				runningApps.add(mcApp);

				JSONObject logsJso = appJso.getJSONObject("logs");
				// The logs object always exists, but can be empty.
				if (logsJso.has("build")) {
					String buildLogPath = logsJso.getJSONObject("build").getString("file");
					mcApp.addLogPath(buildLogPath);
				}

				if (logsJso.has("app")) {
					// TODO I'm not sure if this will always be an array, but it seems to be for Liberty projects
					JSONArray appLogsJso = logsJso.getJSONObject("app").getJSONArray("file");
					for (int j = 0; j < appLogsJso.length(); j++) {
						String appLogPath = appLogsJso.getString(j);
						mcApp.addLogPath(appLogPath);
					}
				}
			}
			catch(JSONException e) {
				MCLogger.logError("Error parsing project json: " + appJso, e);
			}
		}

		displayAppsStartingMsg(appsStillStarting);

		return runningApps;
	}

	/**
	 * If an app is still starting up when the app list is being read, the "ports" object will be an empty string,
	 * and getting the "exposedPort" string from it will throw a JSONException (see above).
	 * In this case the user has to wait for the app to start up before it will show up in the wizard,
	 * so here we display a message to let them know why their apps are missing.
	 *
	 * @param appsStarting - Names of apps that have not yet bound to a port.
	 */
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

		Util.openDialog(false, "Applications still starting", appsStartingMsg);
	}

	// Getters for our project state fields

	public boolean isLinked() {
		return isLinked;
	}

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

	public void setLinked(boolean isLinked) {
		MCLogger.log("App " + name + " is now linked? " + isLinked);
		this.isLinked = isLinked;

		com.ibm.microclimate.core.Activator.getDefault().getPreferenceStore()
			.setValue(isLinkedPrefsKey, isLinked);
	}

	/**
	 * @param pathStr - The log path as extracted from the JSON, ie starting with /microclimate-workspace/
	 */
	private void addLogPath(String pathStr) {
		IPath path = mcConnection.localWorkspacePath.append(pathStr);
		MCLogger.log("Add log path " + path);
		logPaths.add(path);
	}

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
		return String.format("%s@%s id=%s name=%s type=%s loc=%s",
				MicroclimateApplication.class.getSimpleName(), rootUrl.toString(),
				projectID, name, projectType, fullLocalPath.toOSString());
	}
}
