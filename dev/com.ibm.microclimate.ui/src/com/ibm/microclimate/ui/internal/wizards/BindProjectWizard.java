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

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

public class BindProjectWizard extends Wizard implements INewWizard {

	private ProjectSelectionPage projectPage;
	private LanguageSelectionPage languagePage;
	
	private MicroclimateConnection connection = null;
	private IProject project = null;
	
	// If a connection is passed in and no project then the project selection page will be shown
	public BindProjectWizard(MicroclimateConnection connection) {
		super();
		this.connection = connection;
		init();
	}
	
	// If the project is passed in then the project selection page will not be shown
	public BindProjectWizard(IProject project) {
		super();
		this.project = project;
		init();
	}
	
	private void init() {
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(MicroclimateUIPlugin.getImageDescriptor(MicroclimateUIPlugin.MICROCLIMATE_BANNER));
		setHelpAvailable(false);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Empty
	}

	@Override
	public void addPages() {
		setWindowTitle("Add Project to Codewind");
		if (project == null) {
			projectPage = new ProjectSelectionPage(connection);
			addPage(projectPage);
		}
		languagePage = new LanguageSelectionPage(connection);
		addPage(languagePage);
	}

	@Override
	public boolean canFinish() {
		boolean canFinish = languagePage.canFinish();
		if (projectPage != null) {
			canFinish &= projectPage.canFinish();
		}
		return canFinish;
	}

	@Override
	public boolean performCancel() {
		MicroclimateConnection newConnection = languagePage.getConnection();
		if (newConnection != null && MicroclimateConnectionManager.getActiveConnection(newConnection.baseUrl.toString()) == null) {
			newConnection.close();
		}
		return true;
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}
		
		MicroclimateConnection newConnection = languagePage.getConnection();
		if (newConnection == null) {
			MCLogger.logError("The connection was null for the add project wizard");
			return false;
		}
		
		if (projectPage != null) {
			project = projectPage.getProject();
		}

		try {
			newConnection.requestProjectValidate(project.getLocation().toFile().getAbsolutePath());
			newConnection.requestProjectBind(project.getName(), project.getLocation().toFile().getAbsolutePath(), languagePage.getLanguage(), languagePage.getType());
			if (MicroclimateConnectionManager.getActiveConnection(newConnection.baseUrl.toString()) == null) {
				MicroclimateConnectionManager.add(newConnection);
			}
			ViewHelper.openMicroclimateExplorerView();
			ViewHelper.refreshMicroclimateExplorerView(null);
			ViewHelper.expandConnection(newConnection);
		} catch (Exception e) {
			MCLogger.logError("An error occured trying to add the project to Codewind: " + project.getName(), e);
			MCUtil.openDialog(true, "Project Add Error",
					"An error occurred trying to add the " + project.getName() + " project to Codewind: " + e.getMessage());
			return false;
		}

		return true;
	}
}
