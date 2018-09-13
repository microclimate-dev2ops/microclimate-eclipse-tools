package com.ibm.microclimate.test;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class MicroclimateTestPlugin extends Plugin {

    private static MicroclimateTestPlugin plugin;
    
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		super.stop(bundleContext);
	}
	
    public static MicroclimateTestPlugin getDefault() {
        return plugin;
    }

}
