package com.ibm.microclimate.core.internal;

import java.io.Closeable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.HttpUtil.HttpResult;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;

/**
 *
 * @author timetchells@ibm.com
 */
public class MicroclimateConnection implements Closeable {

	public static final String MICROCLIMATE_WORKSPACE_PROPERTY = "com.ibm.microclimate.internal.workspace";
	private static final String UNKNOWN_VERSION = "unknown";

	public final String host;
	public final int port;

	public final String baseUrl;
	public final IPath localWorkspacePath;

	private final MicroclimateSocket mcSocket;

	private List<MicroclimateApplication> apps = Collections.emptyList();

	MicroclimateConnection (String host, int port) throws ConnectException, URISyntaxException, JSONException {
		String baseUrl_ = buildUrl(host, port);

		if (MicroclimateConnectionManager.getConnection(baseUrl_) != null) {
			onInitFail("Microclimate Connection at " + baseUrl_ + " already exists.");
		}

		if(!test(baseUrl_)) {
			// Note this message is displayed directly to the user, so we have to localize it.
			onInitFail(String.format("Connecting to Microclimate instance at \"%s\" failed", baseUrl_));
		}

		this.host = host;
		this.port = port;
		this.baseUrl = baseUrl_;

		// Must set host field before doing this
		mcSocket = new MicroclimateSocket(this);
		if(!mcSocket.blockUntilFirstConnection()) {
			// Note this message is displayed directly to the user, so we have to localize it.
			onInitFail(String.format(
					"Connecting to Microclimate Socket at \"%s\" failed",
					mcSocket.socketUri.toString()));
		}

		JSONObject env = getEnvData(this.baseUrl);

		String version = getMCVersion(env);

		if (UNKNOWN_VERSION.equals(version)) {
			onInitFail(String.format("Your version of Microclimate could not be determined. "
					+ "At least version %s is required.",
					MCConstants.REQUIRED_MC_VERSION));
		}
		else if (!isSupportedVersion(version)) {
			onInitFail(String.format("Your version of Microclimate is not supported. "
					+ "You are running version %s but at least version %s is required.",
					version, MCConstants.REQUIRED_MC_VERSION));
		}

		this.localWorkspacePath = getWorkspacePath(env);
		if (localWorkspacePath == null) {
			// Can't recover from this
			// This should never happen since we have already determined it is a supported version of microclimate.
			onInitFail("Could not determine the location of your Microclimate workspace.");
		}

		refreshApps();

		MCLogger.log("Created " + this);
	}

	private void onInitFail(String msg) throws ConnectException {
		MCLogger.log("Initializing MicroclimateConnection failed: " + msg);
		close();
		throw new ConnectException(msg);
	}

	/**
	 * Call this when the connection is removed.
	 */
	@Override
	public void close() {
		MCLogger.log("Disposing of " + this);
		if (mcSocket.socket.connected()) {
			mcSocket.socket.disconnect();
		}
	}

	private static String buildUrl(String host, int port) {
		return String.format("http://%s:%d/", host, port);
	}

	private static boolean test(String baseUrl) {
		String getResult = null;
		try {
			getResult = HttpUtil.get(baseUrl).response;
		} catch (IOException e) {
			MCLogger.logError(e);
			return false;
		}

		return getResult != null && getResult.contains("Microclimate");
	}

	private static JSONObject getEnvData(String baseUrl) throws JSONException {
		final String envUrl = baseUrl + MCConstants.APIPATH_ENV;

		String envResponse = null;
		try {
			envResponse = HttpUtil.get(envUrl).response;
		} catch (IOException e) {
			MCLogger.logError("Error contacting Environment endpoint", e);
			MCUtil.openDialog(true, "Error contacting Microclimate server", "Failed to contact " + envUrl);
			return null;
		}

		return new JSONObject(envResponse);
	}

	private static String getMCVersion(JSONObject env) {
		if (!env.has(MCConstants.KEY_ENV_MC_VERSION)) {
			MCLogger.logError("Missing version from env data");
			return UNKNOWN_VERSION;
		}

		try {
			String versionStr = env.getString(MCConstants.KEY_ENV_MC_VERSION);
			MCLogger.log("Microclimate Version is: " + versionStr);
			return versionStr;
		} catch (JSONException e) {
			// we already checked for this key so I'm not sure what could cause this.
			MCLogger.logError(e);
			return UNKNOWN_VERSION;
		}
	}

	private static boolean isSupportedVersion(String versionStr) {
		if (UNKNOWN_VERSION.equals(versionStr)) {
			return false;
		}

		if ("latest".equals(versionStr)) {
			// Development build - possible other values to check for?
			return true;
		}

		// The version will have a format like '1809', which corresponds to v18.09
		try {
			int version = Integer.parseInt(versionStr);
			return version >= MCConstants.REQUIRED_MC_VERSION;
		}
		catch(NumberFormatException e) {
			MCLogger.logError("Couldn't parse version number from " + versionStr);
			return false;
		}
	}

	private static Path getWorkspacePath(JSONObject env) throws JSONException {
		// Try the internal system property first
		String path = System.getProperty(MICROCLIMATE_WORKSPACE_PROPERTY, null);
		if (path != null && !path.isEmpty()) {
			return new Path(path);
		}

		if (!env.has(MCConstants.KEY_ENV_WORKSPACE_LOC)) {
			MCLogger.logError("Missing workspace location from env data");
			return null;
		}
		String workspaceLoc = env.getString(MCConstants.KEY_ENV_WORKSPACE_LOC);
		return new Path(workspaceLoc);
	}

	/**
	 * Refresh this connection's list of apps from the Microclimate project list endpoint.
	 */
	public void refreshApps() {

		final String projectsUrl = baseUrl + MCConstants.APIPATH_PROJECT_LIST;

		String projectsResponse = null;
		try {
			projectsResponse = HttpUtil.get(projectsUrl).response;
		} catch (IOException e) {
			MCLogger.logError("Error contacting Projects endpoint", e);
			MCUtil.openDialog(true, "Error contacting Microclimate server", "Failed to contact " + projectsUrl);
			return;
		}

		try {
			apps = MicroclimateApplication.getAppsFromProjectsJson(this, projectsResponse);
			MCLogger.log("App list update success");
		}
		catch(Exception e) {
			MCUtil.openDialog(true, "Error getting list of projects", e.getMessage());
		}
	}

	public List<MicroclimateApplication> getApps() {
		refreshApps();
		return apps;
	}

	public List<MicroclimateApplication> getLinkedApps() {
		if (apps == null) {
			return Collections.emptyList();
		}

		return apps.stream()
				.filter(app -> app.isLinked())
				.collect(Collectors.toList());
	}

	/*
	public boolean hasLinkedApp() {
		if (apps == null) {
			return false;
		}

		return apps.stream()
				.anyMatch(app -> app.isLinked());
	}*/

	/**
	 * @return The app with the given ID, if it exists in this Microclimate instance, else null.
	 */
	public MicroclimateApplication getAppByID(String projectID) {
		refreshApps();
		for (MicroclimateApplication app : apps) {
			if (app.projectID.equals(projectID)) {
				return app;
			}
		}

		MCLogger.logError("No project found with ID " + projectID);
		return null;
	}

	public void requestProjectRestart(MicroclimateApplication app, String launchMode)
			throws JSONException, IOException {

        String url = baseUrl + MCConstants.APIPATH_PROJECTS_BASE + "/" + app.projectID + "/" + MCConstants.APIPATH_RESTART;

		JSONObject restartProjectPayload = new JSONObject();
		restartProjectPayload.put(MCConstants.KEY_START_MODE, launchMode);

		// This initiates the restart
		HttpUtil.post(url, restartProjectPayload);
		app.invalidatePorts();
	}

	/**
	 * Get the project status endpoint, and filter the response for the JSON corresponding to the given project.
	 * @return
	 * 	The JSON containing the status info for the given project,
	 * 	or null if the project is not found in the status info.
	 */
	public JSONObject requestProjectStatus(MicroclimateApplication app) throws IOException, JSONException {
		final String statusUrl = baseUrl + MCConstants.APIPATH_PROJECT_LIST;

		HttpResult result = HttpUtil.get(statusUrl);

		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s",
					result.responseCode, result.error);
			throw new IOException(msg);
		}
		else if (result.response == null) {
			// I don't think this will ever happen.
			throw new IOException("Server returned good response code, but null response when getting initial state");
		}

		JSONArray allProjectStatuses = new JSONArray(result.response);
		for (int i = 0; i < allProjectStatuses.length(); i++) {
			JSONObject projectStatus = allProjectStatuses.getJSONObject(i);
			if (projectStatus.getString(MCConstants.KEY_PROJECT_ID).equals(app.projectID)) {
				// Success - found the project of interest
				return projectStatus;
			}
		}

		MCLogger.log("Didn't find status info for project " + app.name);
		return null;
	}

	/**
	 * Called by the MicroclimateSocket when the socket.io connection goes down.
	 */
	public synchronized void onConnectionError() {
		MCLogger.log("MCConnection to " + baseUrl + " lost");

		for (MicroclimateApplication app : getLinkedApps()) {
			MicroclimateServerBehaviour server = app.getLinkedServer();
			if (server != null) {
				// All linked apps should have getLinkedServer return non-null.
				server.onMicroclimateDisconnect(baseUrl);
			}
		}
	}

	/**
	 * Called by the MicroclimateSocket when the socket.io connection is working.
	 */
	public synchronized void clearConnectionError() {
		MCLogger.log("MCConnection to " + baseUrl + " restored");

		for (MicroclimateApplication app : getLinkedApps()) {
			MicroclimateServerBehaviour server = app.getLinkedServer();
			if (server != null) {
				// All linked apps should have getLinkedServer return non-null.
				server.onMicroclimateReconnect();
			}
		}
	}

	@Override
	public String toString() {
		return String.format("%s host=%s port=%d baseUrl=%s workspacePath=%s numApps=%d",
				MicroclimateConnection.class.getSimpleName(), host, port, baseUrl, localWorkspacePath, apps.size());
	}

	// Note that toPrefsString and fromPrefsString are used to save and load connections from the preferences store
	// in MicroclimateConnectionManager, so be careful modifying these.

	private static final String HOST_KEY = "$host", PORT_KEY = "$port";

	public String toPrefsString() {
		// No newlines allowed!

		return String.format("%s %s=%s %s=%s", MicroclimateConnection.class.getSimpleName(),
				HOST_KEY, host, PORT_KEY, port);
	}

	public static MicroclimateConnection fromPrefsString(String str)
			throws ConnectException, NumberFormatException, StringIndexOutOfBoundsException,
			URISyntaxException, JSONException {

		int hostIndex = str.indexOf(HOST_KEY);
		String afterHostKey = str.substring(hostIndex + HOST_KEY.length() + 1);

		// The hostname is between HOST_KEY and the PORT_KEY, also trim the space between them
		String host = afterHostKey.substring(0, afterHostKey.indexOf(PORT_KEY)).trim();

		int portIndex = str.indexOf(PORT_KEY);
		String portStr = str.substring(portIndex + PORT_KEY.length() + 1);

		int port = Integer.parseInt(portStr);

		return new MicroclimateConnection(host, port);
	}
}
