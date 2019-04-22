/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action for deleting a Microclimate project.  If a project exists in the
 * workspace with the same name and location as the Microclimate project,
 * it will be deleted as well.
 */
public class DeleteProjectAction extends SelectionProviderAction {
	
	MCEclipseApplication app;
	
	public DeleteProjectAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.DeleteProjectLabel);
		selectionChanged(getStructuredSelection());
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE));
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MCEclipseApplication) {
				app = (MCEclipseApplication) obj;
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (app == null) {
			// should not be possible
			MCLogger.logError("DeleteProjectAction ran but no application was selected");
			return;
		}

		if (MCUtil.openConfirmDialog(Messages.DeleteProjectTitle, NLS.bind(Messages.DeleteProjectMessage, app.name))) {
			try {
				app.mcConnection.requestProjectDelete(app.projectID);
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
				if (project != null && project.exists() && project.getLocation().toFile().equals(app.fullLocalPath.toFile())) {
					project.delete(false, true, new NullProgressMonitor());
				}
			} catch (Exception e) {
				MCLogger.logError("An error occurred deleting the project: " + app.name + ", with id: " + app.projectID, e);
				MCUtil.openDialog(true, Messages.DeleteProjectErrorTitle,
						NLS.bind(Messages.DeleteProjectErrorMsg, new String[] {app.name, e.getMessage()}));
			}
		}
	}
}
