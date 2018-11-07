package com.ibm.microclimate.ui.internal.messages;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.ibm.microclimate.ui.internal.messages.messages"; //$NON-NLS-1$

	public static String LinkWizard_DontShowThisAgain;
	public static String LinkWizard_GenericErrorCreatingServer;
	public static String LinkWizard_LinkedSuccessDialogMsg;
	public static String LinkWizard_LinkSuccessDialogTitle;
	public static String LinkWizard_ShellTitle;

	public static String LinkPage_WizardTitle;
	public static String LinkPage_ConnectionComboLabel;
	public static String LinkPage_ErrMsgFallThrough;
	public static String LinkPage_ErrMsgNoConnection;
	public static String LinkPage_ErrMsgNoMatchingProj;
	public static String LinkPage_ErrMsgNotRunning;
	public static String LinkPage_ErrMsgOnlyMicroprofileSupported;
	public static String LinkPage_ErrMsgProjAlreadyLinked;
	public static String LinkPage_ErrorGettingSelectedProj;
	public static String LinkPage_ManageConnectionBtn;
	public static String LinkPage_ProjNotRunning;
	public static String LinkPage_ProjectInfoLabel;
	public static String LinkPage_ProjectInfoNameLabel;
	public static String LinkPage_ProjectsComboLabel;
	public static String LinkPage_ProjInfoPathLabel;
	public static String LinkPage_ProjInfoTypeLabel;
	public static String LinkPage_ProjInfoUrlLabel;
	public static String LinkPage_RefreshBtn;
	public static String LinkPage_ShellTitle;
	public static String LinkPage_WizardDescription;

	public static String ConnectionPrefsPage_AddBtn;
	public static String ConnectionPrefsPage_LinkedProjectsColumn;
	public static String ConnectionPrefsPage_NoLinkedAppsDisplay;
	public static String ConnectionPrefsPage_PageTitle;
	public static String ConnectionPrefsPage_RemoveBtn;
	public static String ConnectionPrefsPage_ShellTitle;
	public static String ConnectionPrefsPage_TableTitleLabel;
	public static String ConnectionPrefsPage_URLColumn;

	public static String PrefsParentPage_DebugTimeoutLabel;
	public static String PrefsParentPage_ErrInvalidDebugTimeout;
	public static String PrefsParentPage_HidePostLinkDialogLabel;

	public static String NewConnectionPage_ConnectSucceeded;
	public static String NewConnectionPage_ErrAConnectionAlreadyExists;
	public static String NewConnectionPage_ErrCouldNotConnectToMC;
	public static String NewConnectionPage_HostnameLabel;
	public static String NewConnectionPage_NotValidPortNum;
	public static String NewConnectionPage_OnlyLocalhostSupported;
	public static String NewConnectionPage_PortLabel;
	public static String NewConnectionPage_ShellTitle;
	public static String NewConnectionPage_TestConnectionBtn;
	public static String NewConnectionPage_TestToProceed;
	public static String NewConnectionPage_WizardDescription;
	public static String NewConnectionPage_WizardTitle;
	public static String NewConnectionWizard_ShellTitle;

	public static String OpenAppAction_CantOpenNotRunningAppMsg;
	public static String OpenAppAction_CantOpenNotRunningAppTitle;

	public static String StartBuildAction_AppMissingTitle;
	public static String StartBuildAction_AppMissingMsg;
	public static String StartBuildAction_AlreadyBuildingTitle;
	public static String StartBuildAction_AlreadyBuildingMsg;
	
	public static String MicroclimateConnectionLabel;
	
	public static String RestartInDebugMode;
	public static String RestartInRunMode;
	public static String ErrorOnRestartDialogTitle;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
