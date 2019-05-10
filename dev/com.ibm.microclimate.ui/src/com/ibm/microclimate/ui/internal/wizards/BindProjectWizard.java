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

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
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
		languagePage = new LanguageSelectionPage();
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

		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
			    @Override
			    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
	        		MicroclimateConnection connection, newConnection = null;
	        		try {
	        			List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
	        			if (connections.isEmpty()) {
        					newConnection = MicroclimateConnectionManager.createConnection(MicroclimateConnectionManager.DEFAULT_CONNECTION_URL);
        					connection = newConnection;
	        			} else {
	        				connection = connections.get(0);
	        			}
	        			if (!connection.isConnected()) {
	        				MCLogger.log("The connection at " + connection.baseUrl + " is not active. Could not bind project: " + project.getName());
	        				throw new Exception("The connection at " + connection.baseUrl + " is not active. Check that Codewind is running.");
	        			}
	        			if (connection.getAppByName(project.getName()) != null) {
	        				MCLogger.log("The connection at " + connection.baseUrl + " already has a project named: " + project.getName());
	        				throw new Exception("The connection at " + connection.baseUrl + " already has a project named: " + project.getName());
	        			}
	        			connection.requestProjectBind(project.getName(), project.getLocation().toFile().getAbsolutePath(), languagePage.getLanguage(), languagePage.getType());
	        			if (newConnection != null) {
	        				MicroclimateConnectionManager.add(newConnection);
	        			}
	        			Display.getDefault().asyncExec(new Runnable() {
							@Override
							public void run() {
								ViewHelper.openMicroclimateExplorerView();
			        			ViewHelper.refreshMicroclimateExplorerView(null);
			        			ViewHelper.expandConnection(connection);
							}
	        			});
	        		} catch (Exception e) {
	        			if (newConnection != null) {
	        				newConnection.close();
	        			}
	        			MCLogger.logError("Project bind failed for project: " + project.getName(), e);
	        			throw new InvocationTargetException(e, e.getMessage());
	        		}
			    }
			});
		} catch (Exception e) {
			MCUtil.openDialog(true, "Project Add Error",
					"An error occurred trying to add the " + project.getName() + " project to Codewind: " + e.getMessage());
            return false;
		}
		return true;
	}
}
