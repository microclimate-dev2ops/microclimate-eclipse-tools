package com.ibm.microclimate.core.internal;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;

/**
 *
 * @author timetchells@ibm.com
 */
public class MicroclimateConnection {

	public final String host;
	public final int port;

	public final String baseUrl;
	public final IPath localWorkspacePath;

	private final MicroclimateSocket mcSocket;

	private List<MicroclimateApplication> apps;

	MicroclimateConnection (String host, int port) throws ConnectException, URISyntaxException, JSONException {
		String baseUrl_ = buildUrl(host, port);

		if(!test(baseUrl_)) {
			// Note this message is displayed directly to the user, so we have to localize it.
			throw new ConnectException(
					String.format("Connecting to Microclimate instance at \"%s\" failed", baseUrl_));
		}

		this.host = host;
		this.port = port;
		this.baseUrl = baseUrl_;

		// Must set host field before doing this
		mcSocket = new MicroclimateSocket(this);
		if(!mcSocket.blockUntilFirstConnection()) {
			// Note this message is displayed directly to the user, so we have to localize it.
			throw new ConnectException(
					String.format("Connecting to Microclimate Socket at \"%s\" failed", mcSocket.socketUri.toString()));
		}

		this.localWorkspacePath = getWorkspacePath(this.baseUrl);
		MCLogger.log(String.format("Created MCConnection with the following info: "
				+ "host=%s port=%d baseUrl=%s workspacePath=%s",
				host, port, baseUrl, localWorkspacePath));

		refreshApps();
	}

	/**
	 * Call this when the connection is removed.
	 */
	void disconnect() {
		MCLogger.log("Disposing of MCConnection " + this);
		mcSocket.socket.disconnect();
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

	private static Path getWorkspacePath(String baseUrl) throws JSONException {
		final String envUrl = baseUrl + MCConstants.ENVIRONMENT_APIPATH;

		String envResponse = null;
		try {
			envResponse = HttpUtil.get(envUrl).response;
		} catch (IOException e) {
			MCLogger.logError("Received null response from Environment endpoint", e);
			MCUtil.openDialog(true, "Error contacting Microclimate server", "Failed to contact " + envUrl);
			return null;
		}

		JSONObject env = new JSONObject(envResponse);
		String workspaceLoc = env.getString(MCConstants.KEY_ENV_WORKSPACE_LOC);
		return new Path(workspaceLoc);
	}

	/**
	 * Refresh this connection's list of apps from the Microclimate project list endpoint.
	 */
	public void refreshApps() {

		final String projectsUrl = baseUrl + MCConstants.PROJECTS_LIST_APIPATH;

		String projectsResponse = null;
		try {
			projectsResponse = HttpUtil.get(projectsUrl).response;
		} catch (IOException e) {
			MCLogger.logError("Received null response from projects endpoint", e);
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
	public boolean equals(Object other) {
		if(!(other instanceof MicroclimateConnection)) {
			return false;
		}

		MicroclimateConnection otherMcc = (MicroclimateConnection) other;
		return otherMcc.baseUrl.equals(baseUrl);
	}

	// Note that toString and fromString are used to save and load connections from the preferences store
	// in MicroclimateConnectionManager, so be careful modifying these.

	private static final String HOST_KEY = "$host", PORT_KEY = "$port";

	@Override
	public String toString() {
		return String.format("%s %s=%s %s=%s", MicroclimateConnection.class.getSimpleName(),
				HOST_KEY, host, PORT_KEY, port);
	}

	public static MicroclimateConnection fromString(String str)
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
