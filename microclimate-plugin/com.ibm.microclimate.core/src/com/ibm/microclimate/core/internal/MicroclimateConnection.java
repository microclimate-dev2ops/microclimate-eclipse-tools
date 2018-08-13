package com.ibm.microclimate.core.internal;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONException;

import com.ibm.microclimate.core.MCLogger;

public class MicroclimateConnection {

	private String host;
	private int port;

	private String baseUrl;
	private IPath localWorkspacePath;

	MicroclimateConnection (String host, int port) throws ConnectException {
		String baseUrl_ = buildUrl(host, port);

		if(!test(baseUrl_)) {
			throw new ConnectException(
					String.format("Connecting to Microclimate instance at \"%s\" failed", baseUrl_));
		}

		this.host = host;
		this.port = port;
		this.baseUrl = baseUrl_;
		// TODO
		this.localWorkspacePath = new Path("/Users/tim/programs/microclimate/");
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof MicroclimateConnection)) {
			return false;
		}

		MicroclimateConnection otherMcc = (MicroclimateConnection) other;
		return otherMcc.baseUrl.equals(baseUrl);
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

		//MCLogger.log("From " + url + " got:");
		//MCLogger.log(getResult);

		return getResult != null && getResult.contains("Microclimate");
	}

	public List<MicroclimateApplication> apps()
			throws NumberFormatException, JSONException, MalformedURLException {

		String projectsUrl = baseUrl + "api/v1/projects";

		String projectsResponse = null;
		try {
			projectsResponse = HttpUtil.get(projectsUrl).response;
		} catch (IOException e) {
			MCLogger.logError(e);
		}

		if(projectsResponse == null) {
			MCLogger.logError("Received null response from projects endpoint");
			return Collections.emptyList();
		}

		return MicroclimateApplication.buildFromProjectsJson(this, projectsResponse);
	}

	// Getters

	public String host() {
		return host;
	}

	public String baseUrl() {
		return baseUrl;
	}

	public IPath localWorkspacePath() {
		return localWorkspacePath;
	}

	// Note that toString and fromString are used to save and load connections from the preferences store
	// in MicroclimateConnectionManager

	private static final String HOST_KEY = "$host", PORT_KEY = "$port";

	@Override
	public String toString() {
		return String.format("%s %s=%s %s=%s", MicroclimateConnection.class.getSimpleName(),
				HOST_KEY, host, PORT_KEY, port);
	}

	public static MicroclimateConnection fromString(String str)
			throws ConnectException, NumberFormatException, StringIndexOutOfBoundsException {

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
