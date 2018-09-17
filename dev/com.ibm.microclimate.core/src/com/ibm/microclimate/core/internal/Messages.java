package com.ibm.microclimate.core.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.ibm.microclimate.core.internal.messages"; //$NON-NLS-1$

	public static String MicroclimateConnection_ErrConnection_AlreadyExists;
	public static String MicroclimateConnection_ErrConnection_OldVersion;
	public static String MicroclimateConnection_ErrConnection_VersionUnknown;
	public static String MicroclimateConnection_ErrConnection_WorkspaceErr;
	public static String MicroclimateConnection_ErrContactingServerDialogMsg;
	public static String MicroclimateConnection_ErrContactingServerDialogTitle;
	public static String MicroclimateConnection_ErrGettingProjectListTitle;

	public static String MicroclimateConnectionException_ConnectingToMCFailed;

	public static String MicroclimateConnectionManager_CancelBtn;
	public static String MicroclimateConnectionManager_DeleteServersBtn;
	public static String MicroclimateConnectionManager_ErrDeletingServerDialogTitle;
	public static String MicroclimateConnectionManager_ServersLinkedDialogMsg;
	public static String MicroclimateConnectionManager_ServersLinkedDialogTitle;

	public static String MicroclimateReconnectJob_ReconnectErrorDialogMsg;
	public static String MicroclimateReconnectJob_ReconnectErrorDialogTitle;
	public static String MicroclimateReconnectJob_ReconnectJobName;

	public static String MicroclimateServer_CantModifyModules;

	public static String MicroclimateServerBehaviour_CantRestartDialogMsg;
	public static String MicroclimateServerBehaviour_CantRestartDialogTitle;
	public static String MicroclimateServerBehaviour_ConnectionLostServerSuffix;
	public static String MicroclimateServerBehaviour_DebugLaunchConfigName;
	public static String MicroclimateServerBehaviour_DeletingServerModeSwitchMsg;
	public static String MicroclimateServerBehaviour_DeletingServerModeSwitchTitle;
	public static String MicroclimateServerBehaviour_ErrCreatingServerDialogMsg;
	public static String MicroclimateServerBehaviour_ErrCreatingServerDialogTitle;
	public static String MicroclimateServerBehaviour_ErrInitiatingRestartDialogTitle;
	public static String MicroclimateServerBehaviour_ErrMissingAttribute;
	public static String MicroclimateServerBehaviour_ErrSettingInitialStateDialogTitle;
	public static String MicroclimateServerBehaviour_MissingProjectID;
	public static String MicroclimateServerBehaviour_ProjectMissingServerSuffix;
	public static String MicroclimateServerBehaviour_ServerDoesntSupportPublish;
	public static String MicroclimateServerBehaviour_DebuggerConnectFailureDialogTitle;
	public static String MicroclimateServerBehaviour_DebuggerConnectFailureDialogMsg;

	public static String MicroclimateServerConsole_ConsoleName;

	public static String MicroclimateServerLaunchConfigDelegate_ErrInitServer;
	public static String MicroclimateServerLaunchConfigDelegate_CantLaunchNotStarted;

	public static String MicroclimateSocket_ErrRestartingProjectDialogMsg;
	public static String MicroclimateSocket_ErrRestartingProjectDialogTitle;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
