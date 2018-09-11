package com.ibm.microclimate.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.ibm.microclimate.ui.internal.messages"; //$NON-NLS-1$

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

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
