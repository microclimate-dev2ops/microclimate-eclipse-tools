package com.ibm.microclimate.core.internal;

import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MCLogger;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Wrapper for a SocketIO client socket, which connects to FileWatcher and listens for project state changes,
 * then updates the corresponding MicroclimateApplication's state.
 * One of these exists for each MicroclimateConnection. That connection is stored here so we can access
 * its applications.
 *
 * @author timetchells@ibm.com
 *
 */
public class MicroclimateSocket {

	public final Socket socket;

	private final MicroclimateConnection mcConnection;

	// SocketIO Event names
	private static final String
			EVENT_PROJECT_STATUS_CHANGE = "projectStatusChanged",
			EVENT_PROJECT_RESTART = "projectRestartResult";

	public MicroclimateSocket(MicroclimateConnection mcConnection) throws URISyntaxException {
		this.mcConnection = mcConnection;

		// TODO hardcoded filewatcher port
		final String url = "http://" + mcConnection.host + ':' + "9091";
		socket = IO.socket(url);

		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log("SocketIO connect success @ " + url);
			}
		})
		.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.logError("SocketIO Connect Failure @ " + url);

				// TODO In this case we should probably put all applications associated with this socket's MCConnection
				// into Unknown state, and notify the user that this MC instance could not be contacted.
				// It should only happen if Microclimate is not running, or if something is
				// seriously wrong.

				if (arg0[0] instanceof Exception) {
					Exception e = (Exception) arg0[0];
					MCLogger.logError("SocketIO Connect Error", e);
				}
			}
		})
		.on(Socket.EVENT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.logError("SocketIO @ " + url);

				// TODO show somewhere that the connection is down (icon on the server?)
				if (arg0[0] instanceof Exception) {
					Exception e = (Exception) arg0[0];
					MCLogger.logError("SocketIO Error", e);
				}
			}
		})
		.on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				// Don't think this is ever used
				MCLogger.log("SocketIO message " + arg0[0].toString());
			}
		})
		// Below are Filewatcher-specific events
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

		MCLogger.log("Created MicroclimateSocket connected to " + url);
	}

	// Event data JSON constants
	public static final String
			KEY_PROJECT_ID = "projectID",

			KEY_STATUS = "status",
			KEY_APP_STATUS = "appStatus",
			KEY_BUILD_STATUS = "buildStatus",
			KEY_DETAILED_BUILD_STATUS = "detailedBuildStatus",

			KEY_PORTS = "ports",
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
		// The project state events will be received separately and handled normally.
		MicroclimateApplication app = getApp(event);
		if (app == null) {
			// can't recover from this
			MCLogger.logError("MCApplication not found, aborting state update triggered by restart");
			return;
		}

		String status = event.getString(KEY_STATUS);
		if (!REQUEST_STATUS_SUCCESS.equalsIgnoreCase(status)) {
			MCLogger.log("Project restart failed on the server: " + event.toString());
			return;
		}

		// this event should always have a 'ports' sub-object
		JSONObject portsObj = event.getJSONObject("ports");

		// ports object should always have an http port
		int port = parsePort(portsObj.getString(KEY_EXPOSED_HTTP_PORT));
		if (port != -1) {
			app.setHttpPort(port);
		}

		// Debug port will obviously be missing if the restart was into Run mode.
		if (portsObj.has(KEY_EXPOSED_DEBUG_PORT)) {
			int debugPort = parsePort(portsObj.getString(KEY_EXPOSED_DEBUG_PORT));
			if (debugPort != -1) {
				app.setDebugPort(debugPort);
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
