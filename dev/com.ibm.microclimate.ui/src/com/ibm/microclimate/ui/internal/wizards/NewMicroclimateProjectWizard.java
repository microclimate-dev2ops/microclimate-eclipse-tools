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

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.IOperationHandler;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.console.ProjectTemplateInfo;
import com.ibm.microclimate.ui.internal.actions.ImportProjectAction;
import com.ibm.microclimate.ui.internal.messages.Messages;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

public class NewMicroclimateProjectWizard extends Wizard {

	private final MicroclimateConnection connection;
	private final List<ProjectTemplateInfo> templateList;;
	private NewMicroclimateProjectPage newProjectPage;
	
	public NewMicroclimateProjectWizard(MicroclimateConnection connection, List<ProjectTemplateInfo> templateList) {
		this.connection = connection;
		this.templateList = templateList;
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.NewProjectPage_ShellTitle);
		newProjectPage = new NewMicroclimateProjectPage(connection, templateList);
		addPage(newProjectPage);
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}

		ProjectTemplateInfo info = newProjectPage.getProjectTemplateInfo();
		String name = newProjectPage.getProjectName();
		if (info == null || name == null) {
			MCLogger.logError("The project type or name was null for the new project wizard");
			return false;
		}
		
		try {
			final boolean importProject = newProjectPage.importProject();
			connection.getMCSocket().registerProjectCreateHandler(name, new IOperationHandler() {
				@Override
				public void operationComplete(boolean passed, String msg) {
					connection.getMCSocket().deregisterProjectCreateHandler(name);
					if (passed) {
						MicroclimateApplication app = connection.getAppByName(name);
						if (app != null) {
							Display.getDefault().asyncExec(new Runnable() {
								@Override
								public void run() {
									ViewHelper.expandConnection(connection);
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
			connection.requestProjectCreate(info, name);
			return true;
		} catch (Exception e) {
			MCLogger.logError("An error occured trying to create a project with type: " + info.getExtension() + ", and name: " + name, e);
			MCUtil.openDialog(true, Messages.NewProjectPage_ProjectCreateErrorTitle,
					NLS.bind(Messages.NewProjectPage_ProjectCreateErrorMsg, new String[] {name, e.getMessage()}));
			return false;
		}
	}
}
