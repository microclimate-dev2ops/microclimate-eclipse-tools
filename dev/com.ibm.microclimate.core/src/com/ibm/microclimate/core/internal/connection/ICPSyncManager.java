/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.core.internal.connection;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.remote.Syncthing;

public class ICPSyncManager {
	
	// Eventually should change this to list the projects that are synced
	// per connection so can show it in a preferences page and allow users
	// to control synced projects there as well
	public static final String SYNC_ENABLED_PREFSKEY = "microclimate-sync-enabled"; //$NON-NLS-1$
	
	public String setupLocalProject(MicroclimateApplication app) throws Exception {
		// Get syncthing instance
		Syncthing syncthing = MicroclimateCorePlugin.getInstance().getSyncthing();
		if (!syncthing.isRunning()) {
			syncthing.start();
		}
		
		// Set up synchronization for the application
		String host = app.mcConnection.getHost();
		String namespace = app.mcConnection.getSocketNamespace();
		String localFolder = syncthing.shareICPFolder(host, namespace, app.name);
		setSyncEnabled(true);
		return localFolder;
	}
	
	public void initSynchronization() throws Exception {
		if (isSyncEnabled()) {
			Syncthing syncthing = MicroclimateCorePlugin.getInstance().getSyncthing();
			if (!syncthing.isRunning()) {
				syncthing.start();
			}
			syncthing.restore();
		}
	}
	
	public boolean isSyncEnabled() {
		return MicroclimateCorePlugin.getDefault()
		.getPreferenceStore()
		.getBoolean(SYNC_ENABLED_PREFSKEY);
	}
	
	public void setSyncEnabled(boolean value) {
		MicroclimateCorePlugin.getDefault()
		.getPreferenceStore().setValue(SYNC_ENABLED_PREFSKEY, value);
	}

}
