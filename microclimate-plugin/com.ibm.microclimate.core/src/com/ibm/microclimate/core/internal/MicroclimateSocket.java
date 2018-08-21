package com.ibm.microclimate.core.internal;

import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.server.AppStateConverter;
import com.ibm.microclimate.core.server.MicroclimateServerBehaviour;

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
			EVENT_PROJECT_RESTART = "projectRestartResult",
			EVENT_PROJECT_DELETION = "projectDeletion";

	public MicroclimateSocket(MicroclimateConnection mcConnection) throws URISyntaxException {
		this.mcConnection = mcConnection;

		// TODO hardcoded filewatcher port
		final String url = "http://" + mcConnection.host + ':' + "9091";
		socket = IO.socket(url);

		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log("SocketIO connect success @ " + url);

				mcConnection.clearConnectionError();
			}
		})
		.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				if (arg0[0] instanceof Exception) {
					Exception e = (Exception) arg0[0];
					MCLogger.logError("SocketIO Connect Error @ " + url, e);
				}
				mcConnection.onConnectionError();
			}
		})
		.on(Socket.EVENT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				if (arg0[0] instanceof Exception) {
					Exception e = (Exception) arg0[0];
					MCLogger.logError("SocketIO Error @ " + url, e);
				}
			}
		})
		.on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				// Don't think this is ever used
				MCLogger.log("SocketIO EVENT_MESSAGE " + arg0[0].toString());
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
		})
		.on(EVENT_PROJECT_DELETION, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				MCLogger.log(EVENT_PROJECT_DELETION + ": " + arg0[0].toString());

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectDeletion(event);
				} catch (JSONException e) {
					MCLogger.logError("Error parsing JSON: " + arg0[0].toString(), e);
				}
			}
		});

		socket.connect();

		MCLogger.log("Created MicroclimateSocket connected to " + url);
	}

	private void onProjectStatusChanged(JSONObject event) throws JSONException {
		MicroclimateServerBehaviour serverBehaviour = getServerForEvent(event);
		if (serverBehaviour == null) {
			// This is OK. It will happen if the project that's changed is not linked to a server.
			return;
		}

		updateServerState(serverBehaviour, event);
	}

	public static void updateServerState(MicroclimateServerBehaviour serverBehaviour, JSONObject projectChangedEvent)
			throws JSONException {

		serverBehaviour.clearSuffix();
		if (projectChangedEvent.has(MCConstants.KEY_APP_STATUS)) {
			serverBehaviour.onAppStateUpdate(projectChangedEvent.getString(MCConstants.KEY_APP_STATUS));
		}
		// These are mutually exclusive - it won't have both an app status and a build status.
		else if (projectChangedEvent.has(MCConstants.KEY_BUILD_STATUS)) {
			serverBehaviour.onBuildStateUpdate(
					projectChangedEvent.getString(MCConstants.KEY_BUILD_STATUS),
					projectChangedEvent.getString(MCConstants.KEY_DETAILED_BUILD_STATUS));
		}
		else {
			MCLogger.log("No state of interest to update");
		}
	}

	private void onProjectRestart(JSONObject event) throws JSONException {
		// The restart event doesn't provide a project state. We just update the ports here
		// The project state events will be received separately and handled normally.
		MicroclimateServerBehaviour serverBehaviour = getServerForEvent(event);
		if (serverBehaviour == null) {
			MCLogger.logError("Failed to get serverBehaviour, aborting state update triggered by restart");
		}

		String status = event.getString(MCConstants.KEY_STATUS);
		if (!MCConstants.REQUEST_STATUS_SUCCESS.equalsIgnoreCase(status)) {
			MCLogger.log("Project restart failed on the server: " + event.toString());
			MCUtil.openDialog(true, "Error restarting project",
					"Failed to restart server " + serverBehaviour.getServer().getName() + ": " + status);
			return;
		}

		// this event should always have a 'ports' sub-object
		JSONObject portsObj = event.getJSONObject(MCConstants.KEY_PORTS);

		// ports object should always have an http port
		int port = parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_HTTP_PORT));
		if (port != -1) {
			serverBehaviour.getApp().setHttpPort(port);
		}

		// Debug port will obviously be missing if the restart was into Run mode.
		if (portsObj.has(MCConstants.KEY_EXPOSED_DEBUG_PORT)) {
			int debugPort = parsePort(portsObj.getString(MCConstants.KEY_EXPOSED_DEBUG_PORT));
			if (debugPort != -1) {
				serverBehaviour.getApp().setDebugPort(debugPort);
			}
		}
	}

	private void onProjectDeletion(JSONObject event) throws JSONException {
		MicroclimateServerBehaviour serverBehaviour = getServerForEvent(event);
		if (serverBehaviour == null) {
			MCLogger.logError("Failed to get serverBehaviour, aborting projectDeletion status update");
			return;
		}

		serverBehaviour.onAppStateUpdate(AppStateConverter.APPSTATE_STOPPED);
		serverBehaviour.setSuffix("Project has been deleted or disabled in Microclimate", true);
	}

	private MicroclimateServerBehaviour getServerForEvent(JSONObject event) throws JSONException {
		String projectID = event.getString(MCConstants.KEY_PROJECT_ID);
		MicroclimateApplication app = mcConnection.getAppByID(projectID);
		if (app == null) {
			// can't recover from this
			// This is normal if the project has been deleted or disabled
			MCLogger.logError("MCApplication with ID " + projectID + " not found");
			return null;
		}

		MicroclimateServerBehaviour server = app.getLinkedServer();
		if (server == null) {
			// This happens when the project to be updated doesn't have a linked server. Do nothing in this case.
			return null;
		}

		return server;
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
