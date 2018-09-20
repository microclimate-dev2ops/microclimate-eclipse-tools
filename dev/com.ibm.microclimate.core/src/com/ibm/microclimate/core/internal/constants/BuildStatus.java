package com.ibm.microclimate.core.internal.constants;

public enum BuildStatus {

	IN_PROGRESS("inProgress"),
	SUCCESS("success"),
	FAILED("failed"),
	QUEUED("queued"),
	UNKNOWN("unknown");

	public final String status;

	/**
	 * @param buildStatus - Internal build status used by Microclimate
	 */
	private BuildStatus(String buildStatus) {
		this.status = buildStatus;
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
