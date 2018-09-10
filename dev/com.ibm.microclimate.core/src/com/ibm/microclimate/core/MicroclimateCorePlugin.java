/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;

/**
 * The activator class controls the plug-in life cycle
 */
public class MicroclimateCorePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.microclimate.core"; //$NON-NLS-1$

	public static final String DEFAULT_ICON_PATH = "icons/microclimate.ico";

	public static final String
			// Boolean option for hiding the on-finish dialog for the Link Project wizard
			HIDE_ONFINISH_MSG_PREFSKEY = "showLinkWizardOnFinishDialog",
			// Int option for debug timeout in seconds
			DEBUG_CONNECT_TIMEOUT_PREFSKEY = "serverDebugTimeout";

	// The shared instance
	private static MicroclimateCorePlugin plugin;

	/**
	 * The constructor
	 */
	public MicroclimateCorePlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		// Register our logger with the debug options service
		context.registerService(DebugOptionsListener.class, MCLogger.instance(), null);

		// Set default preferences once, here
		getPreferenceStore().setDefault(HIDE_ONFINISH_MSG_PREFSKEY, false);
		getPreferenceStore().setDefault(DEBUG_CONNECT_TIMEOUT_PREFSKEY,
				MicroclimateServerBehaviour.DEFAULT_DEBUG_CONNECT_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MicroclimateCorePlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getIcon(String path) {
		final URL url = MicroclimateCorePlugin.getDefault().getBundle().getEntry(DEFAULT_ICON_PATH);
		return ImageDescriptor.createFromURL(url);
	}

}
