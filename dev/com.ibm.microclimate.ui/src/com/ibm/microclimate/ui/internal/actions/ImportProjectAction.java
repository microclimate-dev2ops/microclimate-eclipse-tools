/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action for importing a Microclimate project into Eclipse.  This makes
 * the source available for editing and debugging.
 */
@SuppressWarnings("restriction")
public class ImportProjectAction implements IObjectActionDelegate {

	protected MicroclimateApplication app;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			action.setEnabled(false);
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateApplication) {
				app = (MicroclimateApplication) obj;
				if (app.isAvailable()) {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
					action.setEnabled(project == null || !project.exists());
					return;
				}
			}
		}
		action.setEnabled(false);
	}

	@Override
	public void run(IAction action) {
		if (app == null) {
			// should not be possible
			MCLogger.logError("ImportProjectAction ran but no application was selected");
			return;
		}

		importProject(app);
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
	
	/**
	 * Import a Microclimate project into Eclipse using Smart Import.
	 */
	public static void importProject(MicroclimateApplication app) {
		try {
			IPath path = app.fullLocalPath;
			SmartImportJob importJob = new SmartImportJob(path.toFile(), null, true, false);
			importJob.schedule();
		} catch (Exception e) {
			MCLogger.logError("Error importing project: " + app.name, e); //$NON-NLS-1$
			MCUtil.openDialog(true, NLS.bind(Messages.ImportProjectError, app.name), e.getMessage());
			return;
		}
	}
}
