/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal.constants;

public class MCConstants {

	private MCConstants() {}

	public static final String

			BUILD_LOG_SHORTNAME = "build.log",

			MC_SERVER_BASE_NAME = "Microclimate Project - ",

			// Version string returned by development builds of MC
			VERSION_LATEST = "latest",

			// Portal API endpoints
			APIPATH_PROJECT_LIST = "api/v1/projects",
			APIPATH_ENV = "api/v1/environment",
			APIPATH_RESTART = "restart",
			APIPATH_BUILD = "build",
			APIPATH_CLOSE = "close",
			APIPATH_OPEN = "open",

			// JSON keys
			KEY_PROJECT_ID = "projectID",
			KEY_NAME = "name",
			// KEY_PROJECT_TYPE = "projectType",
			KEY_BUILD_TYPE = "buildType",
			KEY_LOC_DISK = "locOnDisk",
			KEY_CONTEXTROOT = "contextroot",

			KEY_BUILD_LOG = "build-log",
			KEY_BUILD_LOG_LAST_MODIFIED = "build-log-last-modified",
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
			KEY_AUTO_BUILD = "autoBuild",

			KEY_ENV_WORKSPACE_LOC = "workspace_location",
			KEY_ENV_MC_VERSION = "microclimate_version",

			KEY_LANGUAGE = "language",
			KEY_FRAMEWORK = "framework",

			KEY_START_MODE = "startMode",
			KEY_ACTION = "action",
			VALUE_ACTION_BUILD = "build",
			VALUE_ACTION_ENABLEAUTOBUILD = "enableautobuild",
			VALUE_ACTION_DISABLEAUTOBUILD = "disableautobuild",

			// JSON attribute values
			REQUEST_STATUS_SUCCESS = "success"

			;

	// Microclimate 18.09 is required
	// otherwise we will be missing the Workspace ENV data and the project restart endpoint.
	public static final int
			// This version supports only Liberty projects.
			REQUIRED_MC_VERSION = 1809,
			// This version supports both Spring and Liberty projects.
			SPRING_MC_VERSION = 1810;

}
