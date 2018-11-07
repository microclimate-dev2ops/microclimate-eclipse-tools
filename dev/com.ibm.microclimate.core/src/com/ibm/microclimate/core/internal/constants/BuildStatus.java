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

	public boolean equals(String s) {
		return this.name().equals(s);
	}

	public static final String BUILD_INPROGRESS_SUFFIX = "Build In Progress";

	/**
	 * Convert a raw buildStatus to a user-friendly one.
	 */
	public static String toUserFriendly(String buildStatus, String detailedBuildStatus) {
		detailedBuildStatus = detailedBuildStatus.trim();

		if (IN_PROGRESS.equals(buildStatus)) {
			String inProgress = BUILD_INPROGRESS_SUFFIX;
			if (detailedBuildStatus != null && !detailedBuildStatus.isEmpty()) {
				inProgress += " - " + detailedBuildStatus;
			}
			return inProgress;
		}
		else if (SUCCESS.equals(buildStatus)) {
			return "Build Succeeded";
		}
		else if (FAILED.equals(buildStatus)) {
			if (detailedBuildStatus == null || detailedBuildStatus.isEmpty()) {
				detailedBuildStatus = "Please check " + MCConstants.BUILD_LOG_SHORTNAME;
			}

			return "Build Failed - " + detailedBuildStatus;
		}
		else if (QUEUED.equals(buildStatus)) {
			return "Build Queued";
		}
		else {
			// could be "unknown", or could be something else
			String building = "Building";
			if (detailedBuildStatus != null && !detailedBuildStatus.isEmpty()) {
				building = building + " - " + detailedBuildStatus;
			}
			return building;
		}
	}
}
