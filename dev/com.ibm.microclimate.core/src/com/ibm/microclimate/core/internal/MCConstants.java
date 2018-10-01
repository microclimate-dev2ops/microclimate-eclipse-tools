/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import org.eclipse.wst.server.core.IServer;

public class MCConstants {

	private MCConstants() {}

	public static final String

			BUILD_LOG_SHORTNAME = "build.log",

			MC_SERVER_BASE_NAME = "Microclimate Project - ",

			// Portal API endpoints
			APIPATH_PROJECTS_BASE = "api/v1/projects",
			APIPATH_PROJECT_LIST = "api/v1/projects",
			APIPATH_ENV = "api/v1/environment",
			APIPATH_RESTART = "restart",
			APIPATH_BUILD = "build",

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

			KEY_OPEN_STATE = "state",
			// VALUE_STATE_OPEN = "open",
			VALUE_STATE_CLOSED = "closed",

			KEY_STATUS = "status",
			KEY_APP_STATUS = "appStatus",
			KEY_BUILD_STATUS = "buildStatus",
			KEY_DETAILED_BUILD_STATUS = "detailedBuildStatus",
			KEY_PORTS = "ports",
			KEY_EXPOSED_PORT = "exposedPort",
			KEY_EXPOSED_DEBUG_PORT = "exposedDebugPort",

			KEY_ENV_WORKSPACE_LOC = "workspace_location",
			KEY_ENV_MC_VERSION = "microclimate_version",

			KEY_LANGUAGE = "language",
			KEY_FRAMEWORK = "framework",

			KEY_START_MODE = "startMode",

			KEY_ACTION = "action",
			VALUE_ACTION_BUILD = "build",

			// JSON attribute values
			REQUEST_STATUS_SUCCESS = "success",

			BUILD_STATUS_INPROGRESS = "inProgress",
			BUILD_STATUS_SUCCESS = "success",
			BUILD_STATUS_FAILED = "failed",
			BUILD_STATUS_QUEUED = "queued",
			BUILD_STATUS_UNKNOWN = "unknown"
			;

	// Microclimate 18.09 is required
	// otherwise we will be missing the Workspace ENV data and the project restart endpoint.
	public static final int REQUIRED_MC_VERSION = 1809;

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

	public static final String BUILD_IN_PROGRESS_SUFFIX = "Build In Progress";

	/**
	 * Convert a raw buildStatus to a user-friendly one.
	 */
	public static String buildStateToUserFriendly(String buildStatus, String detailedBuildStatus) {
		detailedBuildStatus = detailedBuildStatus.trim();

		if (MCConstants.BUILD_STATUS_INPROGRESS.equals(buildStatus)) {
			String inProgress = BUILD_IN_PROGRESS_SUFFIX;
			if (detailedBuildStatus != null && !detailedBuildStatus.isEmpty()) {
				inProgress += " - " + detailedBuildStatus;
			}
			return inProgress;
		}
		else if (MCConstants.BUILD_STATUS_SUCCESS.equals(buildStatus)) {
			return "Build Succeeded";
		}
		else if (MCConstants.BUILD_STATUS_FAILED.equals(buildStatus)) {
			if (detailedBuildStatus == null || detailedBuildStatus.isEmpty()) {
				detailedBuildStatus = "Please check " + MCConstants.BUILD_LOG_SHORTNAME;
			}

			return "Build Failed - " + detailedBuildStatus;
		}
		else if (MCConstants.BUILD_STATUS_QUEUED.equals(buildStatus)) {
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

	public static final String

			PROJECT_TYPE_LIBERTY = "liberty",
			PROJECT_TYPE_SPRING = "spring",
			PROJECT_TYPE_NODE = "nodejs",
			PROJECT_TYPE_SWIFT = "swift",
			PROJECT_TYPE_UNKNOWN = "unknown",

			// user-friendly project types
			USER_PROJECT_TYPE_LIBERTY = "Microprofile",
			USER_PROJECT_TYPE_SPRING = "Spring",
			USER_PROJECT_TYPE_NODE = "Node.js",
			USER_PROJECT_TYPE_SWIFT = "Swift",
			USER_PROJECT_TYPE_UNKNOWN = "Unknown";

	public static String projectTypeToUserFriendly(String projectType) {
		if (PROJECT_TYPE_LIBERTY.equals(projectType)) {
			return USER_PROJECT_TYPE_LIBERTY;
		}
		else if (PROJECT_TYPE_SPRING.equals(projectType)) {
			return USER_PROJECT_TYPE_SPRING;
		}
		else if (PROJECT_TYPE_NODE.equals(projectType)) {
			return USER_PROJECT_TYPE_NODE;
		}
		else if (PROJECT_TYPE_SWIFT.equals(projectType)) {
			return USER_PROJECT_TYPE_SWIFT;
		}
		else if (PROJECT_TYPE_UNKNOWN.equals(projectType)) {
			return USER_PROJECT_TYPE_UNKNOWN;
		}
		else {
			MCLogger.logError("Unknown project type: " + projectType);
			return projectType;
		}
	}
}
