/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.constants;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.messages.Messages;

public enum AppState {

	STARTED	("started", Messages.AppStateStarted),
	STARTING("starting", Messages.AppStateStarting),
	STOPPING("stopping", Messages.AppStateStopping),
	STOPPED	("stopped", Messages.AppStateStopped),
	UNKNOWN	("unknown", Messages.AppStateUnknown);

	public final String appState;
	public final String displayString;

	/**
	 * @param appState - App state used by Microclimate
	 */
	private AppState(String appState, String displayString) {
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

	public String getDisplayString(StartMode mode) {
		if (this == AppState.STARTED && StartMode.DEBUG_MODES.contains(mode)) {
			return Messages.AppStateDebugging;
		}
		return displayString;
	}
	
}
