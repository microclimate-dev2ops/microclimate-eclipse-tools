package com.ibm.microclimate.core.internal.constants;

import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.internal.MCLogger;

public enum AppState {

	STARTED	(IServer.STATE_STARTED, "started"),
	STARTING(IServer.STATE_STARTING, "starting"),
	STOPPING(IServer.STATE_STOPPING, "stopping"),
	STOPPED	(IServer.STATE_STOPPED, "stopped"),
	UNKNOWN	(IServer.STATE_UNKNOWN, "unknown");

	public final int serverState;
	public final String appState;

	/**
	 * @param serverState - Server state used by Server framework
	 * @param appState - App state used by Microclimate
	 */
	private AppState(int serverState, String appState) {
		this.serverState = serverState;
		this.appState = appState;
	}

	/**
	 * Convert an IServer state to a human-readable Microclimate AppState.
	 */
	public static String convert(int serverState) {
		for (AppState state : AppState.values()) {
			if (state.serverState == serverState) {
				return state.appState;
			}
		}
		MCLogger.logError("Unrecognized server state " + serverState);
		return null;
	}

	/**
	 * Convert a Microclimate AppState to an IServer State.
	 */
	public static int convert(String appState) {
		for (AppState state : AppState.values()) {
			if (state.appState.equals(appState)) {
				return state.serverState;
			}
		}
		MCLogger.logError("Unrecognized server state " + appState);
		return -1;
	}
}
