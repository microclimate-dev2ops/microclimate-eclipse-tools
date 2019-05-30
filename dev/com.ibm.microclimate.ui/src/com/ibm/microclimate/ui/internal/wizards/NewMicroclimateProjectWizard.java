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

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.json.JSONException;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.IOperationHandler;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.console.ProjectTemplateInfo;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
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

		final ProjectTemplateInfo info = newProjectPage.getProjectTemplateInfo();
		final String name = newProjectPage.getProjectName();
		if (info == null || name == null) {
			MCLogger.logError("The project type or name was null for the new project wizard");
			return false;
		}
		
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
							IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
							if (project == null || !project.exists()) {
								ImportProjectAction.importProject(app);
							} else {
								// This should not happen since the wizard checks for this
								MCLogger.logError("The project cannot be imported because a project already exists with the name: " + app.name);
							}
						}
						return;
					} else {
						MCLogger.logError("An import operation was requested but the application could not be found for: " + name);
					}
				}
			}
		});
		Job job = new Job(NLS.bind(Messages.NewProjectWizard_CreateProjectJobTitle, name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					connection.requestProjectCreate(info, name);
					return Status.OK_STATUS;
				} catch (Exception e) {
					MCLogger.logError("An error occurred creating project: " + name, e);
					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, NLS.bind(Messages.NewProjectWizard_ProjectCreateErrorMsg, name), e);
				}
			}
		};
		job.schedule();
		return true;
	}
}
