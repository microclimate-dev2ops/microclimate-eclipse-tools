/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
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
