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

import java.util.EnumSet;

import org.json.JSONObject;

import com.ibm.microclimate.core.internal.MCLogger;

/**
 * Project start modes.
 */
public enum StartMode {

	RUN("run"),
	DEBUG("debug"),
	DEBUG_NO_INIT("debugNoInit");
	
	public static final EnumSet<StartMode> DEBUG_MODES = EnumSet.of(DEBUG, DEBUG_NO_INIT);

	public final String startMode;

	/**
	 * @param buildStatus - Internal build status used by Microclimate
	 */
	private StartMode(String startMode) {
		this.startMode = startMode;
	}

	public boolean equals(String s) {
		return this.name().equals(s);
	}
	
	public static StartMode get(String startMode) {
		for (StartMode mode : StartMode.values()) {
			if (mode.startMode.equals(startMode)) {
				return mode;
			}
		}
		return null;
	}
	
	public static StartMode get(JSONObject obj) {
		try {
			String mode = null;
			if (obj.has(MCConstants.KEY_START_MODE)) {
				mode = obj.getString(MCConstants.KEY_START_MODE);
			}
			if (mode == null) {
				MCLogger.log("No start mode was specified on JSON object");
				return StartMode.RUN;
			} else {
				StartMode startMode = StartMode.get(mode);
				if (startMode == null) {
					MCLogger.log("Unrecognized start mode: " + mode);
					return StartMode.RUN;
				}
				return startMode;
			}
		} catch (Exception e) {
			MCLogger.logError("Failed to get start mode", e);
		}
		return StartMode.RUN;
	}
}