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
			APIPATH_VALIDATE = "api/v1/validate",
			APIPATH_VALIDATE_GENERATE = "api/v1/validate/generate",
			APIPATH_RESTART = "restart",
			APIPATH_BUILD = "build",
			APIPATH_CLOSE = "close",
			APIPATH_OPEN = "open",
			APIPATH_CAPABILITIES = "capabilities",

			// JSON keys
			KEY_PROJECT_ID = "projectID",
			KEY_NAME = "name",
			KEY_PROJECT_TYPE = "projectType",
			KEY_BUILD_TYPE = "buildType",
			KEY_LOC_DISK = "locOnDisk",
			KEY_CONTEXTROOT = "contextroot",
			KEY_CONTAINER_ID = "containerId",

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
			VALUE_ACTION_DELETING = "deleting",
			VALUE_ACTION_VALIDATING = "validating",
			
			KEY_VALIDATION_STATUS = "validationStatus",
			KEY_VALIDATION_RESULTS = "validationResults",
			KEY_SEVERITY = "severity",
			KEY_FILENAME = "filename",
			KEY_FILEPATH = "filepath",
			KEY_TYPE = "type",
			KEY_LABEL = "label",
			KEY_DETAILS = "details",
			KEY_QUICKFIX = "quickfix",
			KEY_FIXID = "fixID",
			KEY_DESCRIPTION = "description",
			VALUE_STATUS_SUCCESS = "success",
			VALUE_STATUS_FAILED = "failed",
			VALUE_SEVERITY_ERROR = "error",
			VALUE_SEVERITY_WARNING = "warning",
			VALUE_TYPE_MISSING = "missing",
			VALUE_TYPE_INVALID = "invalid",
			
			KEY_CAPABILIITES = "capabilities",
			KEY_START_MODES = "startModes",
			KEY_CONTROL_COMMANDS = "controlCommands",
			
			KEY_AUTO_GENERATE = "autoGenerate",

			// JSON attribute values
			REQUEST_STATUS_SUCCESS = "success",
			
			// Microclimate files
			DOCKERFILE = "Dockerfile",
			DOCKERFILE_BUILD = "Dockerfile-build",
			
			QUERY_NEW_PROJECT = "new-project",
			VALUE_TRUE = "true",
			
			QUERY_PROJECT = "project",
			QUERY_VIEW = "view",
			VIEW_MONITOR = "monitor",
			VIEW_OVERVIEW = "overview"

			;

	// Microclimate 18.12 is required as earlier versions are missing needed APIs.
	public static final int
			REQUIRED_MC_VERSION = 1812;

}
