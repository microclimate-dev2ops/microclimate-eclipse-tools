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

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.internal.actions.OpenMicroclimateUIAction.Page;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action provider for a Microclimate connection.
 */
public class MicroclimateConnectionActionProvider extends CommonActionProvider {
	
	private OpenMicroclimateUIAction openUIHomePageAction;
	private OpenMicroclimateUIAction openNewProjectPageAction;
	private OpenMicroclimateUIAction openImportProjectPageAction;
	private NewProjectAction newProjectAction;
	
	@Override
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		ISelectionProvider selProvider = aSite.getStructuredViewer();
		openUIHomePageAction = new OpenMicroclimateUIAction(Messages.OpenUIAction_Label, Page.HOME, selProvider);
		openNewProjectPageAction = new OpenMicroclimateUIAction(Messages.OpenUINewProjectAction_Label, Page.NEW_PROJECT, selProvider);
		openImportProjectPageAction = new OpenMicroclimateUIAction(Messages.OpenUIImportProjectAction_Label, Page.IMPORT_PROJECT, selProvider);
		newProjectAction = new NewProjectAction(selProvider);
	}
	
	@Override
	public void fillContextMenu(IMenuManager menu) {
		final ICommonViewerSite viewSite = getActionSite().getViewSite();
		ISelection selection = viewSite.getSelectionProvider().getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof MicroclimateConnection) {
				MicroclimateConnection connection = (MicroclimateConnection)obj;
				if (connection.checkVersion(1905, "2019_M5_E")) {
					menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, openUIHomePageAction);
					menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, newProjectAction);
					menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, openImportProjectPageAction);
				} else {
					menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, openUIHomePageAction);
					menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, openNewProjectPageAction);
					menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, openImportProjectPageAction);
				}
			}
		}
	}

}
