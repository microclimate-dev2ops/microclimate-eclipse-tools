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

package com.ibm.microclimate.ui.internal.marker;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IMarkerResolution;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.messages.Messages;

public class MicroclimateMarkerResolution implements IMarkerResolution {
	
	private final MicroclimateApplication app;
	private final String quickFixId;
	private final String quickFixDescription;
	
	public MicroclimateMarkerResolution(MicroclimateApplication app, String quickFixId, String quickFixDescription) {
		this.app = app;
		this.quickFixId = quickFixId;
		this.quickFixDescription = quickFixDescription;
	}

	@Override
	public String getLabel() {
		return quickFixDescription;
	}

	@Override
	public void run(IMarker marker) {
		// Some day there should be an API that takes the quick fix id and executes
		// it.  For now, just make a regenerate request.
		try {
			app.mcConnection.requestValidateGenerate(app);
			IResource resource = marker.getResource();
			if (resource != null) {
				Job job = new Job(NLS.bind(Messages.refreshResourceJobLabel, resource.getName())) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				            return Status.OK_STATUS;
						} catch (Exception e) {
							MCLogger.logError("An error occurred while refreshing the resource: " + resource.getLocation()); //$NON-NLS-1$
							return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID,
									NLS.bind(Messages.RefreshResourceError, resource.getLocation()), e);
						}
					}
				};
				job.setPriority(Job.LONG);
				job.schedule();
			}
		} catch (Exception e) {
			MCLogger.logError("The generate request failed for application: " + app.name, e); //$NON-NLS-1$
		}
	}
}
