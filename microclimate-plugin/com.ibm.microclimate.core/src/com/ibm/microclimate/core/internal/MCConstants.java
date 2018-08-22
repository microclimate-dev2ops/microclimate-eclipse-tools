package com.ibm.microclimate.core.internal;

import org.eclipse.wst.server.core.IServer;

public class MCConstants {

	private MCConstants() {}

	public static final String

			BUILD_LOG_SHORTNAME = "build.log",

			PROJECT_TYPE_LIBERTY = "liberty",

			// App statuses
			APPSTATE_STARTED = "started",
			APPSTATE_STARTING = "starting",
			APPSTATE_STOPPING = "stopping",
			APPSTATE_STOPPED = "stopped",
			APPSTATE_UNKNOWN = "unknown",

			// JSON keys
			KEY_PROJECT_ID = "projectID",
			KEY_NAME = "name",
			KEY_PROJECT_TYPE = "projectType",
			KEY_LOC_DISK = "locOnDisk",
			KEY_CONTEXTROOT = "contextroot",

			KEY_LOGS = "logs",
			KEY_LOG_BUILD = "build",
			KEY_LOG_APP = "app",
			KEY_LOG_FILE = "file",

			KEY_STATUS = "status",
			KEY_APP_STATUS = "appStatus",
			KEY_BUILD_STATUS = "buildStatus",
			KEY_DETAILED_BUILD_STATUS = "detailedBuildStatus",
			KEY_PORTS = "ports",
			KEY_EXPOSED_PORT = "exposedPort",
			KEY_EXPOSED_DEBUG_PORT = "exposedDebugPort",

			// JSON attribute values
			REQUEST_STATUS_SUCCESS = "success",

			BUILD_STATUS_INPROGRESS = "inProgress",
			BUILD_STATUS_SUCCESS = "success",
			BUILD_STATUS_FAILED = "failed",
			BUILD_STATUS_UNKNOWN = "unknown"
			;

	/**
	* Convert a Microclimate App State string into the corresponding IServer.STATE constant.
	*/
	public static int appStatusToServerState(String appStatus) {
		if (MCConstants.APPSTATE_STARTED.equals(appStatus)) {
			return IServer.STATE_STARTED;
		}
		else if (MCConstants.APPSTATE_STARTING.equals(appStatus)) {
			return IServer.STATE_STARTING;
		}
		else if (MCConstants.APPSTATE_STOPPING.equals(appStatus)) {
			return IServer.STATE_STOPPING;
		}
		else if (MCConstants.APPSTATE_STOPPED.equals(appStatus)) {
			return IServer.STATE_STOPPED;
		}
		else {
			if (!MCConstants.APPSTATE_UNKNOWN.equals(appStatus)) {
				MCLogger.logError("Unrecognized AppStatus " + appStatus);
			}
			return IServer.STATE_UNKNOWN;
		}
	}

	/**
	* Convert an IServer.STATE constant into the corresponding Microclimate App State string.
	*/
	public static String serverStateToAppStatus(int serverState) {
		if (serverState == IServer.STATE_STARTED) {
			return MCConstants.APPSTATE_STARTED;
		}
		else if (serverState == IServer.STATE_STARTING) {
			return MCConstants.APPSTATE_STARTING;
		}
		else if (serverState == IServer.STATE_STOPPING) {
			return MCConstants.APPSTATE_STOPPING;
		}
		else if (serverState == IServer.STATE_STOPPED) {
			return MCConstants.APPSTATE_STOPPED;
		}
		else {
			return MCConstants.APPSTATE_UNKNOWN;
		}
	}

	/**
	 * Convert a raw buildStatus to a user-friendly one.
	 */
	public static String buildStateToUserFriendly(String buildStatus, String detailedBuildStatus) {
		if (MCConstants.BUILD_STATUS_INPROGRESS.equals(buildStatus)) {
			String inProgress = "Build In Progress";
			if (detailedBuildStatus != null && !detailedBuildStatus.isEmpty()) {
				inProgress += " - " + detailedBuildStatus;
			}
			return inProgress;
		}
		else if (MCConstants.BUILD_STATUS_SUCCESS.equals(buildStatus)) {
			return "Build Succeeded";
		}
		else if (MCConstants.BUILD_STATUS_FAILED.equals(buildStatus)) {
			return "Build Failed - Please check " + MCConstants.BUILD_LOG_SHORTNAME;
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
