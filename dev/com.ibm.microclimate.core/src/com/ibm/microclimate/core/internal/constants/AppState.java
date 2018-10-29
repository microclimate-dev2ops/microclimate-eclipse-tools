/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.constants;

import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.messages.Messages;

public enum AppState {

	STARTED	(IServer.STATE_STARTED, "started", Messages.AppStateStarted),
	STARTING(IServer.STATE_STARTING, "starting", Messages.AppStateStarting),
	STOPPING(IServer.STATE_STOPPING, "stopping", Messages.AppStateStopping),
	STOPPED	(IServer.STATE_STOPPED, "stopped", Messages.AppStateStopped),
	UNKNOWN	(IServer.STATE_UNKNOWN, "unknown", Messages.AppStateUnknown);

	public final int serverState;
	public final String appState;
	public final String displayString;

	/**
	 * @param serverState - Server state used by Server framework
	 * @param appState - App state used by Microclimate
	 */
	private AppState(int serverState, String appState, String displayString) {
		this.serverState = serverState;
		this.appState = appState;
		this.displayString = displayString;
	}
	
	public static AppState get(String appState) {
		for (AppState state : AppState.values()) {
			if (state.appState.equals(appState)) {
				return state;
			}
		}
		MCLogger.logError("Unrecognized application state: " + appState);
		return AppState.UNKNOWN;
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
	
	public String getDisplayString(StartMode mode) {
		if (this == AppState.STARTED && StartMode.DEBUG_MODES.contains(mode)) {
			return Messages.AppStateDebugging;
		}
		return displayString;
	}
	
}
