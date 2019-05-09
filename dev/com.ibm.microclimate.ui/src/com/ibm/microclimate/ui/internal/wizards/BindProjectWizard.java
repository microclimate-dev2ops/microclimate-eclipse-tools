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
import com.ibm.microclimate.ui.internal.messages.Messages;
import com.ibm.microclimate.ui.internal.views.ViewHelper;

public class BindProjectWizard extends Wizard implements INewWizard {

	private ProjectSelectionPage projectPage;
	private LanguageSelectionPage languagePage;
	
	private IProject project;
	
	public BindProjectWizard() {
		this(null);
	}
	
	public BindProjectWizard(IProject project) {
		super();
		this.project = project;
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDefaultPageImageDescriptor(MicroclimateUIPlugin.getImageDescriptor(MicroclimateUIPlugin.MICROCLIMATE_BANNER));
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		setWindowTitle("Add Project to Codewind");
		if (project == null) {
			projectPage = new ProjectSelectionPage();
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
	        				MCLogger.logError("The connection at " + connection.baseUrl + " is not active.");
	        				throw new Exception("The connection at " + connection.baseUrl + " is not active. Check that Codewind is running.");
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
	        			throw new InvocationTargetException(e);
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
