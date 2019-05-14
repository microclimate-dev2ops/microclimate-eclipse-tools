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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.core.internal.console.ProjectTemplateInfo;
import com.ibm.microclimate.core.internal.constants.ProjectType;
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
		
		Job job = new Job("Creating Codewind project: " + name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					newConnection.requestProjectCreate(info, name);
					String type = null;
					if (ProjectType.LANGUAGE_JAVA.equals(info.getLanguage())) {
						if (info.getExtension().toLowerCase().contains("spring")) {
							type = "spring";
						} else if (info.getExtension().toLowerCase().contains("microprofile")) {
							type = "liberty";
						} else {
							type = "docker";
						}
					} else if (ProjectType.LANGUAGE_NODEJS.equals(info.getLanguage())) {
						type = "nodejs";
					} else if (ProjectType.LANGUAGE_SWIFT.equals(info.getLanguage())) {
						type = "swift";
					} else {
						type = "docker";
					}
					newConnection.requestProjectBind(name, newConnection.getWorkspacePath() + "/" + name, info.getLanguage(), type);
					if (MicroclimateConnectionManager.getActiveConnection(newConnection.baseUrl.toString()) == null) {
						MicroclimateConnectionManager.add(newConnection);
					}
					newConnection.refreshApps(null);
					MicroclimateApplication app = newConnection.getAppByName(name);
					if (app != null) {
						ImportProjectAction.importProject(app);
					} else {
						MCLogger.logError("Could not get the application for import: " + name);
					}
					ViewHelper.openMicroclimateExplorerView();
					ViewHelper.refreshMicroclimateExplorerView(newConnection);
					ViewHelper.expandConnection(newConnection);
					return Status.OK_STATUS;
				} catch (Exception e) {
					MCLogger.logError("An error occured trying to create a project with type: " + info.getExtension() + ", and name: " + name, e);
					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, NLS.bind(Messages.NewProjectPage_ProjectCreateErrorMsg, name), e);
				}
			}
		};
		job.schedule();
		return true;
	}
}
