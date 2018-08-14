package com.ibm.microclimate.core.internal;

import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MCLogger;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MCSocket {

	public final Socket socket;

	private final MicroclimateConnection mcConnection;

	private static final String
			EVENT_PROJECT_STATUS_CHANGE = "projectStatusChanged",
			EVENT_PROJECT_RESTART = "projectRestartResult";

	public MCSocket(MicroclimateConnection mcConnection) throws URISyntaxException {
		this.mcConnection = mcConnection;

		socket = IO.socket(mcConnection.baseUrl);

		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log("SocketIO connect success @ " + mcConnection.baseUrl);
			}
		})
		.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log("SocketIO Connect Failure @ " + mcConnection.baseUrl);
			}
		})
		.on(Socket.EVENT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.logError("SocketIO @ " + mcConnection.baseUrl + " Error: " + arg0[0].toString());
			}
		})
		.on(EVENT_PROJECT_STATUS_CHANGE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_STATUS_CHANGE + ": " + arg0[0].toString());

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectStatusChanged(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e);
				}
			}
		})
		.on(EVENT_PROJECT_RESTART, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_RESTART + ": " + arg0[0].toString());

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectRestart(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e);
				}
			}
		});
		socket.connect();

		MCLogger.log("Created SocketIO socket at " + mcConnection.baseUrl);
	}

	// TODO move these to a 'constants' file ?
	public static final String
			KEY_PROJECT_ID = "projectID",

			KEY_STATUS = "status",
			KEY_APP_STATUS = "appStatus",
			KEY_BUILD_STATUS = "buildStatus",
			KEY_DETAILED_BUILD_STATUS = "detailedBuildStatus",

			KEY_EXPOSED_HTTP_PORT = "exposedPort",
			KEY_EXPOSED_DEBUG_PORT = "exposedDebugPort",

			REQUEST_STATUS_SUCCESS = "success";

	private void onProjectStatusChanged(JSONObject event) throws JSONException {
		MicroclimateApplication app = getApp(event);
		if (app == null) {
			// can't recover from this
			MCLogger.logError("MCApplication not found, aborting state update");
			return;
		}

		if (event.has(KEY_APP_STATUS)) {
			app.setAppStatus(event.getString(KEY_APP_STATUS));
		}
		// These are mutually exclusive - it won't have both an app status and a build status.
		else if (event.has(KEY_BUILD_STATUS)) {
			app.setBuildStatus(event.getString(KEY_BUILD_STATUS));
			app.setBuildStatusDetail(event.getString(KEY_DETAILED_BUILD_STATUS));
		}
		else {
			// JSON doesn't match expected format
			MCLogger.logError("Did not find one of the expected keys in projectStatusChanged event: "
					+ event.toString());
		}
	}

	private void onProjectRestart(JSONObject event) throws JSONException {
		// The restart event doesn't provide a project state. We just update the ports here
		MicroclimateApplication app = getApp(event);
		if (app == null) {
			// can't recover from this
			MCLogger.logError("MCApplication not found, aborting state update triggered by restart");
			return;
		}

		if (event.has(KEY_STATUS)) {
			String status = event.getString(KEY_STATUS);
			if (!REQUEST_STATUS_SUCCESS.equalsIgnoreCase(status)) {
				MCLogger.log("Project restart failed on the server: " + event.toString());
				return;
			}
		}
		else {
			MCLogger.logError("Received Project Restart event without status: " + event.toString());
		}

		// this event should always have an HTTP port
		if (event.has(KEY_EXPOSED_HTTP_PORT)) {
			int port = parsePort(event.getString(KEY_EXPOSED_HTTP_PORT));
			if (port != -1) {
				app.setHttpPort(port);
			}
		}
		else {
			MCLogger.logError("Received Project Restart event without http port: " + event.toString());
		}

		// Debug port will obviously be missing if the restart was into Run mode.
		if (event.has(KEY_EXPOSED_DEBUG_PORT)) {
			int port = parsePort(event.getString(KEY_EXPOSED_DEBUG_PORT));
			if (port != -1) {
				app.setDebugPort(port);
			}
		}
	}

	private MicroclimateApplication getApp(JSONObject event) throws JSONException {
		String projectID = event.getString(KEY_PROJECT_ID);
		return mcConnection.getAppByID(projectID);
	}

	private int parsePort(String portStr) {
		try {
			return Integer.parseInt(portStr);
		}
		catch(NumberFormatException e) {
			MCLogger.logError(String.format("Couldn't parse port from \"%s\"", portStr), e);
			return -1;
		}
	}
}
