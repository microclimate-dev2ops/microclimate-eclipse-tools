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

public enum BuildStatus {
	
	IN_PROGRESS("inProgress", Messages.BuildStateInProgress),
	SUCCESS("success", Messages.BuildStateSuccess),
	FAILED("failed", Messages.BuildStateFailed),
	QUEUED("queued", Messages.BuildStateQueued),
	UNKOWN("unknown", Messages.BuildStateUnknown);
	
	public static final String BUILD_REQUIRED="buildRequired";

	public final String status;
	public final String displayString;

	/**
	 * @param buildStatus - Internal build status used by Microclimate
	 */
	private BuildStatus(String buildStatus, String displayString) {
		this.status = buildStatus;
		this.displayString = displayString;
	}
	
	public static BuildStatus get(String buildStatus) {
		if (BUILD_REQUIRED.equals(buildStatus)) {
			return null;
		}
		for (BuildStatus status : BuildStatus.values()) {
			if (status.status.equals(buildStatus)) {
				return status;
			}
		}
		MCLogger.logError("Unrecognized application state: " + buildStatus);
		return BuildStatus.UNKOWN;
	}
	
	public String getDisplayString() {
		return displayString;
	}

}
