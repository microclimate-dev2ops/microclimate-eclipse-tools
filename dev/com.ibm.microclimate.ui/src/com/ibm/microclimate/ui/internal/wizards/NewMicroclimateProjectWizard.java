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

package com.ibm.microclimate.ui.internal.wizards;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.IOperationHandler;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.console.ProjectTemplateInfo;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.actions.ImportProjectAction;
import com.ibm.microclimate.ui.internal.messages.Messages;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

public class NewMicroclimateProjectWizard extends Wizard implements INewWizard {

	private MicroclimateConnection connection = null;
	private List<ProjectTemplateInfo> templateList = null;
	private NewMicroclimateProjectPage newProjectPage = null;
	
	public NewMicroclimateProjectWizard() {
		setDefaultPageImageDescriptor(MicroclimateUIPlugin.getImageDescriptor(MicroclimateUIPlugin.MICROCLIMATE_BANNER));
		setHelpAvailable(false);
		setNeedsProgressMonitor(true);
	}
	
	public NewMicroclimateProjectWizard(MicroclimateConnection connection, List<ProjectTemplateInfo> templateList) {
		this();
		this.connection = connection;
		this.templateList = templateList;
	}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {
		// Empty
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.NewProjectPage_ShellTitle);
		newProjectPage = new NewMicroclimateProjectPage(connection, templateList);
		addPage(newProjectPage);
	}

	@Override
	public boolean performCancel() {
		MicroclimateConnection newConnection = newProjectPage.getConnection();
		if (newConnection != null && MicroclimateConnectionManager.getActiveConnection(newConnection.baseUrl.toString()) == null) {
			newConnection.close();
		}
		return super.performCancel();
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}

		ProjectTemplateInfo info = newProjectPage.getProjectTemplateInfo();
		String name = newProjectPage.getProjectName();
		MicroclimateConnection newConnection = newProjectPage.getConnection();
		if (info == null || name == null || newConnection == null) {
			MCLogger.logError("The connection, project type or name was null for the new project wizard");
			return false;
		}
		
		try {
			final boolean importProject = newProjectPage.importProject();
			newConnection.getMCSocket().registerProjectCreateHandler(name, new IOperationHandler() {
				@Override
				public void operationComplete(boolean passed, String msg) {
					newConnection.getMCSocket().deregisterProjectCreateHandler(name);
					if (passed) {
						MicroclimateApplication app = newConnection.getAppByName(name);
						if (app != null) {
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									ViewHelper.expandConnection(newConnection);
								}
							});
							if (importProject) {
								ImportProjectAction.importProject(app);
							}
							return;
						}
					}
				}
			});
			newConnection.requestProjectCreate(info, name);
			if (MicroclimateConnectionManager.getActiveConnection(newConnection.baseUrl.toString()) == null) {
				MicroclimateConnectionManager.add(newConnection);
			}
			ViewHelper.openMicroclimateExplorerView();
			ViewHelper.refreshMicroclimateExplorerView(newConnection);
			ViewHelper.expandConnection(newConnection);
			return true;
		} catch (Exception e) {
			MCLogger.logError("An error occured trying to create a project with type: " + info.getExtension() + ", and name: " + name, e);
			MCUtil.openDialog(true, Messages.NewProjectPage_ProjectCreateErrorTitle,
					NLS.bind(Messages.NewProjectPage_ProjectCreateErrorMsg, new String[] {name, e.getMessage()}));
			return false;
		}
	}
}
