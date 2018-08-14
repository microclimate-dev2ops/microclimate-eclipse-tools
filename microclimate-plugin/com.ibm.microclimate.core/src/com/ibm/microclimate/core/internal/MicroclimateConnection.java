package com.ibm.microclimate.core.internal;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.ibm.microclimate.core.MCLogger;

public class MicroclimateConnection {

	public final String host;
	public final int port;

	public final String baseUrl;
	public final IPath localWorkspacePath;

	public final MCSocket socket;

	private List<MicroclimateApplication> apps;

	MicroclimateConnection (String host, int port) throws ConnectException, URISyntaxException {
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
		// TODO
		socket = new MCSocket(this);
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

	public List<MicroclimateApplication> getApps() {

		String projectsUrl = baseUrl + "api/v1/projects";

		String projectsResponse = null;
		try {
			projectsResponse = HttpUtil.get(projectsUrl).response;
		} catch (IOException e) {
			MCLogger.logError(e);
		}

		if(projectsResponse == null) {
			MCLogger.logError("Received null response from projects endpoint");
			// TODO display a message that the Microclimate server appears to be down.
			return Collections.emptyList();
		}

		try {
			apps = MicroclimateApplication.buildFromProjectsJson(this, projectsResponse);
			return apps;
		}
		catch(Exception e) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(),
							"Error getting project list", e.getMessage());
				}

			});
		}
		return Collections.emptyList();
	}

	public MicroclimateApplication getAppByID(String projectID) {
		if (apps == null) {
			getApps();
		}

		for (MicroclimateApplication app : apps) {
			if (app.projectID.equals(projectID)) {
				return app;
			}
		}
		MCLogger.logError("No project found with ID " + projectID);
		return null;
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
