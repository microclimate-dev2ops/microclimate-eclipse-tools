/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.constants;

import java.util.EnumSet;

import org.eclipse.debug.core.ILaunchManager;
import org.json.JSONObject;

import com.ibm.microclimate.core.internal.MCLogger;

public enum StartMode {

	RUN("run", ILaunchManager.RUN_MODE),
	DEBUG("debug", ILaunchManager.DEBUG_MODE),
	DEBUG_INIT("debugInit", ILaunchManager.DEBUG_MODE);
	
	public static final EnumSet<StartMode> DEBUG_MODES = EnumSet.of(DEBUG, DEBUG_INIT);

	public final String startMode;
	public final String launchMode;

	/**
	 * @param buildStatus - Internal build status used by Microclimate
	 */
	private StartMode(String startMode, String launchMode) {
		this.startMode = startMode;
		this.launchMode = launchMode;
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
				MCLogger.logError("No start mode was specified on JSON object");
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