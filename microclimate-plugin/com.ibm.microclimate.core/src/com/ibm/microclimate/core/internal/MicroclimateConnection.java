package com.ibm.microclimate.core.internal;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.ibm.microclimate.core.MCLogger;

/**
 *
 * @author timetchells@ibm.com
 */
public class MicroclimateConnection {

	public final String host;
	public final int port;

	public final String baseUrl;
	public final IPath localWorkspacePath;

	public final MicroclimateSocket socket;

	private List<MicroclimateApplication> apps;

	private static final String PROJECTS_LIST_PATH = "api/v1/projects";

	MicroclimateConnection (String host, int port) throws ConnectException, URISyntaxException {
		String baseUrl_ = buildUrl(host, port);

		if(!test(baseUrl_)) {
			// TODO this message is displayed directly to the user, so we have to localize it.
			throw new ConnectException(
					String.format("Connecting to Microclimate instance at \"%s\" failed", baseUrl_));
		}

		this.host = host;
		this.port = port;
		this.baseUrl = baseUrl_;
		// TODO - Requires Portal API for getting user's workspace on their machine.
		this.localWorkspacePath = new Path("/Users/tim/programs/microclimate/");
		this.socket = new MicroclimateSocket(this);

		getApps();
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

		// TODO improve this!
		return getResult != null && getResult.contains("Microclimate");
	}

	/**
	 * Refresh this connection's list of apps from the Microclimate project list endpoint.
	 * @return The new list of apps, which matches that in the 'apps' private field.
	 */
	public List<MicroclimateApplication> getApps() {

		final String projectsUrl = baseUrl + PROJECTS_LIST_PATH;

		String projectsResponse = null;
		try {
			projectsResponse = HttpUtil.get(projectsUrl).response;
		} catch (IOException e) {
			MCLogger.logError(e);
		}

		if(projectsResponse == null) {
			MCLogger.logError("Received null response from projects endpoint");
			MCUtil.openDialog(true, "Error contacting Microclimate server", "Failed to contact " + projectsUrl);
			return Collections.emptyList();
		}

		try {
			apps = MicroclimateApplication.getAppsFromProjectsJson(this, projectsResponse);
			return apps;
		}
		catch(Exception e) {
			MCUtil.openDialog(true, "Error getting list of projects", e.getMessage());
		}
		return Collections.emptyList();
	}

	public List<MicroclimateApplication> getLinkedApps() {
		return apps.stream()
				.filter(app -> app.isLinked())
				.collect(Collectors.toList());
	}

	public boolean hasLinkedApp() {
		return apps.stream()
				.anyMatch(app -> app.isLinked());
	}

	/**
	 * @return The app with the given ID, if it exists in this Microclimate instance, else null.
	 */
	public MicroclimateApplication getAppByID(String projectID) {
		return getAppByID(projectID, true);
	}

	private MicroclimateApplication getAppByID(String projectID, boolean retry) {
		for (MicroclimateApplication app : apps) {
			if (app.projectID.equals(projectID)) {
				return app;
			}
		}

		if (retry) {
			// Refresh the project list and retry one time in case the project is new.
			getApps();
			return getAppByID(projectID, false);
		}
		MCLogger.logError("No project found with ID " + projectID);

		return null;
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
			throws ConnectException, NumberFormatException, StringIndexOutOfBoundsException, URISyntaxException {

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
