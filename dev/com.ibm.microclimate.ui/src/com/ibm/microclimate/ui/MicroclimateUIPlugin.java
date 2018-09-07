package com.ibm.microclimate.ui;

import java.net.URL;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.ibm.microclimate.core.MicroclimateCorePlugin;

/**
 * The activator class controls the plug-in life cycle
 */
public class MicroclimateUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.microclimate.ui"; //$NON-NLS-1$

	public static final String
			ICON_BASE_PATH = "icons/",
			MICROCLIMATE_ICON_PATH = "microclimate.ico",
			MICROCLIMATE_BANNER_PATH = "microclimateBanner.png",
			ERROR_ICON_PATH = "error.gif";

	// The shared instance
	private static MicroclimateUIPlugin plugin;

	/**
	 * The constructor
	 */
	public MicroclimateUIPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
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
	public static MicroclimateUIPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getIcon(String path) {
		final URL url = MicroclimateUIPlugin.getDefault().getBundle().getEntry(ICON_BASE_PATH + path);
		return ImageDescriptor.createFromURL(url);
	}

	public static ImageDescriptor getDefaultIcon() {
		return getIcon(MICROCLIMATE_ICON_PATH);
	}

	@Override
	/**
	 * @return The core plugin's preference store - everything should be stored there to prevent confusion.
	 */
	public IPreferenceStore getPreferenceStore() {
		return MicroclimateCorePlugin.getDefault().getPreferenceStore();
	}

}
