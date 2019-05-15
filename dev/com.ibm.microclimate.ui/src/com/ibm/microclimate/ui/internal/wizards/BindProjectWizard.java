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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;

public class BindProjectWizard extends Wizard implements INewWizard {

	private ProjectSelectionPage projectPage;
	private LanguageSelectionPage languagePage;
	
	private final MicroclimateConnection connection;
	private IProject project = null;
	
	// If a connection is passed in and no project then the project selection page will be shown
	public BindProjectWizard(MicroclimateConnection connection) {
		super();
		this.connection = connection;
		init();
	}
	
	// If the project is passed in then the project selection page will not be shown
	public BindProjectWizard(MicroclimateConnection connection, IProject project) {
		super();
		this.connection = connection;
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
			projectPage = new ProjectSelectionPage(this, connection);
			addPage(projectPage);
		}
		languagePage = new LanguageSelectionPage(connection, project);
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
		return true;
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}
		
		if (projectPage != null) {
			project = projectPage.getProject();
		}

		Job job = new Job("Adding project to Codewind: " + project.getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					connection.requestProjectBind(project.getName(), project.getLocation().toFile().getAbsolutePath(), languagePage.getLanguage(), languagePage.getType());
					return Status.OK_STATUS;
				} catch (Exception e) {
					MCLogger.logError("An error occured trying to add the project to Codewind: " + project.getName(), e);
					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID, "An error occurred trying to add the " + project.getName() + " project to Codewind.", e);
				}
			}
		};
		job.schedule();

		return true;
	}
	
	public void setProject(IProject project) {
		if (languagePage != null) {
			languagePage.setProject(project);
		}
	}
}
