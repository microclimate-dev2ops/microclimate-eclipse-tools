/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.messages;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.ibm.microclimate.core.internal.messages.messages"; //$NON-NLS-1$

	public static String MicroclimateConnection_ErrConnection_AlreadyExists;
	public static String MicroclimateConnection_ErrConnection_OldVersion;
	public static String MicroclimateConnection_ErrConnection_VersionUnknown;
	public static String MicroclimateConnection_ErrConnection_WorkspaceErr;
	public static String MicroclimateConnection_ErrContactingServerDialogMsg;
	public static String MicroclimateConnection_ErrContactingServerDialogTitle;
	public static String MicroclimateConnection_ErrGettingProjectListTitle;
	public static String MicroclimateConnection_ErrConnection_UpdateCacheException;

	public static String MicroclimateConnectionException_ConnectingToMCFailed;

	public static String MicroclimateReconnectJob_ReconnectErrorDialogMsg;
	public static String MicroclimateReconnectJob_ReconnectErrorDialogTitle;
	public static String MicroclimateReconnectJob_ReconnectJobName;

	public static String MicroclimateServerBehaviour_DebugLaunchConfigName;
	public static String MicroclimateServerBehaviour_DebuggerConnectFailureDialogTitle;
	public static String MicroclimateServerBehaviour_DebuggerConnectFailureDialogMsg;

	public static String BuildConsoleName;
	public static String AppConsoleName;
	public static String LogFileConsoleName;
	public static String LogFileInitialMsg;

	public static String FileNotFoundTitle;
	public static String FileNotFoundMsg;

	public static String MicroclimateSocket_ErrRestartingProjectDialogMsg;
	public static String MicroclimateSocket_ErrRestartingProjectDialogTitle;
	
	public static String AppStateStarting;
	public static String AppStateStarted;
	public static String AppStateStopping;
	public static String AppStateStopped;
	public static String AppStateUnknown;
	public static String AppStateDebugging;
	
	public static String BuildStateQueued;
	public static String BuildStateInProgress;
	public static String BuildStateSuccess;
	public static String BuildStateFailed;
	public static String BuildStateUnknown;
	
	public static String DebugLaunchError;
	public static String ConnectDebugJob;
	
	public static String RefreshResourceJobLabel;
	public static String RefreshResourceError;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
