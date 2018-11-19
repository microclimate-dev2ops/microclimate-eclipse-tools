/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.ui.internal.views.UpdateHandler;

/**
 * The activator class controls the plug-in life cycle
 */
public class MicroclimateUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.ibm.microclimate.ui"; //$NON-NLS-1$

	private static URL ICON_BASE_URL;
	protected Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>();
	
	public static final String
			ICON_BASE_PATH = "icons/",
			ENABLE_LOCAL_BASE_PATH = "icons/elcl16/",
			MICROCLIMATE_ICON = "microclimate.ico",
			MICROCLIMATE_BANNER = "microclimateBanner.png",
			ERROR_ICON_PATH = "error.gif",
			OPEN_APP_ICON = "openApp.gif",
			BUILD_ICON = "build_exec.png",
			IMPORT_ICON = "import_wiz.png",
			LAUNCH_DEBUG_ICON = "launch_debug.gif",
			LAUNCH_RUN_ICON = "launch_run.gif",
			REFRESH_ICON = "refresh.png";

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
		MicroclimateCorePlugin.setUpdateHandler(new UpdateHandler());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		MicroclimateCorePlugin.setUpdateHandler(null);
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

	public static ImageDescriptor getImageDescriptor(String path) {
		final URL url = MicroclimateUIPlugin.getDefault().getBundle().getEntry(ICON_BASE_PATH + path);
		return ImageDescriptor.createFromURL(url);
	}

	public static ImageDescriptor getDefaultIcon() {
		return getImageDescriptor(MICROCLIMATE_ICON);
	}
	
    public static Image getImage(String key) {
        return plugin.getImageRegistry().get(key);
    }
	
    @Override
    protected ImageRegistry createImageRegistry() {
        ImageRegistry registry = new ImageRegistry();
        if (ICON_BASE_URL == null)
            ICON_BASE_URL = plugin.getBundle().getEntry(ICON_BASE_PATH);

        registerImage(registry, MICROCLIMATE_ICON, ICON_BASE_URL + MICROCLIMATE_ICON);
        registerImage(registry, OPEN_APP_ICON, ENABLE_LOCAL_BASE_PATH + OPEN_APP_ICON);
        registerImage(registry, BUILD_ICON, ENABLE_LOCAL_BASE_PATH + BUILD_ICON);
        registerImage(registry, IMPORT_ICON, ENABLE_LOCAL_BASE_PATH + IMPORT_ICON);
        registerImage(registry, LAUNCH_DEBUG_ICON, ENABLE_LOCAL_BASE_PATH + LAUNCH_DEBUG_ICON);
        registerImage(registry, LAUNCH_RUN_ICON, ENABLE_LOCAL_BASE_PATH + LAUNCH_RUN_ICON);
        registerImage(registry, REFRESH_ICON, ENABLE_LOCAL_BASE_PATH + REFRESH_ICON);

        return registry;
    }

    private void registerImage(ImageRegistry registry, String key, String partialURL) {
        try {
            ImageDescriptor id = ImageDescriptor.createFromURL(new URL(ICON_BASE_URL, partialURL));
            registry.put(key, id);
            imageDescriptors.put(key, id);
        } catch (Exception e) {
            MCLogger.logError("Error registering image", e);
        }
    }

	@Override
	/**
	 * @return The core plugin's preference store - everything should be stored there to prevent confusion.
	 */
	public IPreferenceStore getPreferenceStore() {
		return MicroclimateCorePlugin.getDefault().getPreferenceStore();
	}

}
