package com.ibm.microclimate.core.server;

import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.MCLogger;

public class AppStateConverter {

	private AppStateConverter() {}

	public static final String
			APPSTATE_STARTED = "started",
			APPSTATE_STARTING = "starting",
			APPSTATE_STOPPING = "stopping",
			APPSTATE_STOPPED = "stopped",
			APPSTATE_UNKNOWN = "unknown";

	/**
	* Convert a Microclimate App State string into the corresponding IServer.STATE constant.
	*/
	static int appStatusToServerState(String appStatus) {
		if (APPSTATE_STARTED.equals(appStatus)) {
			return IServer.STATE_STARTED;
		}
		else if (APPSTATE_STARTING.equals(appStatus)) {
			return IServer.STATE_STARTING;
		}
		else if (APPSTATE_STOPPING.equals(appStatus)) {
			return IServer.STATE_STOPPING;
		}
		else if (APPSTATE_STOPPED.equals(appStatus)) {
			return IServer.STATE_STOPPED;
		}
		else {
			if (!APPSTATE_UNKNOWN.equals(appStatus)) {
				MCLogger.logError("Unrecognized AppStatus " + appStatus);
			}
			return IServer.STATE_UNKNOWN;
		}
	}

	/**
	* Convert an IServer.STATE constant into the corresponding Microclimate App State string.
	*/
	static String serverStateToAppStatus(int serverState) {
		if (serverState == IServer.STATE_STARTED) {
			return APPSTATE_STARTED;
		}
		else if (serverState == IServer.STATE_STARTING) {
			return APPSTATE_STARTING;
		}
		else if (serverState == IServer.STATE_STOPPING) {
			return APPSTATE_STOPPING;
		}
		else if (serverState == IServer.STATE_STOPPED) {
			return APPSTATE_STOPPED;
		}
		else {
			return APPSTATE_UNKNOWN;
		}
	}

}
