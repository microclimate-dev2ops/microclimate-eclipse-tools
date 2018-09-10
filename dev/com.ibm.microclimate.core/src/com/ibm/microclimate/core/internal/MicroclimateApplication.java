package com.ibm.microclimate.core.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

	private final Set<IPath> logPaths;

	// Must be updated whenever httpPort changes. Can be null
	private URL baseUrl;

	// These are set by the MicroclimateSocket so we have to make sure the reads and writes are synchronized
	// An httpPort of -1 indicates the app is not started - could be building or disabled.
	private int httpPort = -1, debugPort = -1;

	MicroclimateApplication(MicroclimateConnection mcConnection,
			String id, String name, String projectType, String pathInWorkspace,
			int httpPort, String contextRoot)
					throws MalformedURLException {

		this.mcConnection = mcConnection;
		this.projectID = id;
		this.name = name;
		this.projectType = projectType;
		this.httpPort = httpPort;
		this.contextRoot = contextRoot;
		this.host = mcConnection.baseUrl.getHost();
		this.logPaths = new HashSet<>();

		// The mcConnection.localWorkspacePath will end in /microclimate-workspace
		// and the path passed here will start with /microclimate-workspace, so here we fix the duplication.
		this.fullLocalPath = MCUtil.appendPathWithoutDupe(mcConnection.localWorkspacePath, pathInWorkspace);

		setBaseUrl();

		//MCLogger.log("Created mcApp:");
		//MCLogger.log(toString());
	}

	public static MicroclimateServerBehaviour getServerWithProjectID(String projectID) {
		for (IServer server : ServerCore.getServers()) {
			if (projectID.equals(server.getAttribute(MicroclimateServer.ATTR_PROJ_ID, ""))) {
				return server.getAdapter(MicroclimateServerBehaviour.class);
			}
		}
		return null;
	}

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

				String type = MCConstants.PROJECT_TYPE_UNKNOWN;
				try {
					type = appJso.getString(MCConstants.KEY_PROJECT_TYPE);
				}
				catch(JSONException e) {
					// Sometimes (seems to be when project is disabled) this value is missing -
					// see https://github.ibm.com/dev-ex/portal/issues/425
					MCLogger.logError(e.getMessage() + " in: " + appJso);
				}

				String loc = appJso.getString(MCConstants.KEY_LOC_DISK);

				int httpPort = -1;

				// Is the app started?
				// If so, get the port. If not, leave port set to -1, this indicates the app is not started.
				if (appJso.has(MCConstants.KEY_APP_STATUS)
						&& MCConstants.APPSTATE_STARTED.equals(appJso.getString(MCConstants.KEY_APP_STATUS))) {

					try {
						String httpPortStr = appJso.getJSONObject(MCConstants.KEY_PORTS)
								.getString(MCConstants.KEY_EXPOSED_PORT);
						httpPort = Integer.parseInt(httpPortStr);
					}
					catch(JSONException e) {
						// Indicates the app is not started
						MCLogger.log(name + " has not bound to a port");
					}
					catch(NumberFormatException e) {
						MCLogger.logError("Error parsing port from " + appJso, e);
					}
				}
				else {
					MCLogger.log(name + " is not running");
				}

				String contextRoot = null;
				if(appJso.has(MCConstants.KEY_CONTEXTROOT)) {
					contextRoot = appJso.getString(MCConstants.KEY_CONTEXTROOT);
				}

				MicroclimateApplication mcApp =
						new MicroclimateApplication(mcConnection, id, name, type, loc, httpPort, contextRoot);
				apps.add(mcApp);

				if (appJso.has(MCConstants.KEY_LOGS)) {
					JSONObject logsJso = appJso.getJSONObject(MCConstants.KEY_LOGS);
					if (logsJso.has(MCConstants.KEY_LOG_BUILD)) {
						String buildLogPath = logsJso.getJSONObject(MCConstants.KEY_LOG_BUILD)
								.getString(MCConstants.KEY_LOG_FILE);

						mcApp.addLogPath(buildLogPath);
					}

					if (logsJso.has(MCConstants.KEY_LOG_APP)) {
						// TODO I'm not sure if this will always be an array, but it seems to be for Liberty projects
						JSONArray appLogsJso = logsJso.getJSONObject(MCConstants.KEY_LOG_APP)
								.getJSONArray(MCConstants.KEY_LOG_FILE);

						for (int j = 0; j < appLogsJso.length(); j++) {
							String appLogPath = appLogsJso.getString(j);
							mcApp.addLogPath(appLogPath);
						}
					}
				}
				else {
					MCLogger.logError("Project JSON didn't have logs key: " + appJso);
				}
			}
			catch(JSONException e) {
				MCLogger.logError("Error parsing project json: " + appJso, e);
			}
		}

		return apps;
	}

	private void setBaseUrl() throws MalformedURLException {
		if (httpPort == -1) {
			MCLogger.log("Un-setting baseUrl because httpPort is not valid");
			baseUrl = null;
			return;
		}

		baseUrl = new URL("http", host, httpPort, "");

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

	public Set<IPath> getLogFilePaths() {
		return logPaths;
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

	/**
	 * @param pathStr - The log path as extracted from the JSON, ie starting with /microclimate-workspace/
	 */
	private void addLogPath(String pathStr) {
		IPath path = MCUtil.appendPathWithoutDupe(mcConnection.localWorkspacePath, pathStr);
		// MCLogger.log("Add log path " + path);
		logPaths.add(path);
	}

	public synchronized void setHttpPort(int httpPort) {
		MCLogger.log("Set HTTP port for " + baseUrl + " to " + httpPort);
		this.httpPort = httpPort;
		try {
			setBaseUrl();
		} catch (MalformedURLException e) {
			MCLogger.logError(e);
		}
	}

	public synchronized void setDebugPort(int debugPort) {
		MCLogger.log("Set debug port for " + baseUrl + " to " + debugPort);
		this.debugPort = debugPort;
	}

	/**
	 * Invalidate fields that can change when the application is restarted.
	 * On restart success, these will be updated by the Socket handler for that event.
	 * This is done because the server will wait for the ports to be
	 * set to something other than -1 before trying to connect.
	 */
	public synchronized void invalidatePorts() {
		MCLogger.log("Invalidate ports for " + baseUrl.toString());
		httpPort = -1;
		debugPort = -1;
	}

	@Override
	public String toString() {
		return String.format("%s@%s id=%s name=%s type=%s loc=%s",
				MicroclimateApplication.class.getSimpleName(), baseUrl.toString(),
				projectID, name, projectType, fullLocalPath.toOSString());
	}
}
