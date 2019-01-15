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

package com.ibm.microclimate.core;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.ibm.microclimate.core.internal.IDebugLauncher;
import com.ibm.microclimate.core.internal.IUpdateHandler;
import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;

/**
 * The activator class controls the plug-in life cycle
 */
public class MicroclimateCorePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.microclimate.core"; //$NON-NLS-1$

	public static final String DEFAULT_ICON_PATH = "icons/microclimate.ico"; //$NON-NLS-1$

	public static final String
			// Int option for debug timeout in seconds
			DEBUG_CONNECT_TIMEOUT_PREFSKEY = "serverDebugTimeout"; //$NON-NLS-1$
	
	public static final String NODEJS_DEBUG_BROWSER_PREFSKEY = "nodejsDebugBrowserName"; //$NON-NLS-1$

	// The shared instance
	private static MicroclimateCorePlugin plugin;
	
	private static IUpdateHandler updateHandler;
	
	private static Map<String, IDebugLauncher> debugLaunchers = new HashMap<String, IDebugLauncher>();

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
		getPreferenceStore().setDefault(DEBUG_CONNECT_TIMEOUT_PREFSKEY,
				MCEclipseApplication.DEFAULT_DEBUG_CONNECT_TIMEOUT);
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
	
	public static void setUpdateHandler(IUpdateHandler handler) {
		updateHandler = handler;
	}
	
	public static IUpdateHandler getUpdateHandler() {
		return updateHandler;
	}
	
	public static void addDebugLauncher(String language, IDebugLauncher launcher) {
		debugLaunchers.put(language, launcher);
	}
	
	public static IDebugLauncher getDebugLauncher(String language) {
		return debugLaunchers.get(language);
	}

}
