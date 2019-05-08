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

import java.net.URI;
import java.util.List;

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
import com.ibm.microclimate.core.internal.MicroclimateObjectFactory;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.messages.Messages;

public class BindProjectWizard extends Wizard implements INewWizard {

	private LanguageSelectionPage languagePage;
	
	private final IProject project;
	
	public BindProjectWizard(IProject project) {
		super();
		this.project = project;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setDefaultPageImageDescriptor(MicroclimateUIPlugin.getImageDescriptor(MicroclimateUIPlugin.MICROCLIMATE_BANNER));
		setHelpAvailable(false);
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.NewConnectionWizard_ShellTitle);
		languagePage = new LanguageSelectionPage();
		addPage(languagePage);
	}

	@Override
	public boolean canFinish() {
		return languagePage.canFinish();
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

		Job job = new Job("Binding project: " + project.getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MicroclimateConnection newConnection = null;
				try {
					List<MicroclimateConnection> connections = MicroclimateConnectionManager.activeConnections();
					MicroclimateConnection connection = null;
					if (connections.isEmpty()) {
						String uriStr = "http://localhost:9090";
						try {
							newConnection = MicroclimateObjectFactory.createMicroclimateConnection(new URI(uriStr));
							connection = newConnection;
						} catch (Exception e) {
							MCLogger.logError("An error occurred trying to connect to Codewind at: " + uriStr, e);
							return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID,
									"An error occurred trying connect to Codewind at: " + uriStr, e);
						}
					} else {
						connection = connections.get(0);
					}
					if (!connection.isConnected()) {
						MCLogger.logError("The connection at " + connection.baseUrl + " is not active.");
						return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID,
								"The connection at " + connection.baseUrl + " is not active. Check that Codewind is running.");
					}
					connection.requestProjectBind(project.getName(), project.getLocation().toFile().getAbsolutePath(), languagePage.getLanguage(), languagePage.getType());
					if (newConnection != null) {
						MicroclimateConnectionManager.add(newConnection);
					}
					return Status.OK_STATUS;
				} catch (Exception e) {
					if (newConnection != null) {
						newConnection.close();
					}
					MCLogger.logError("Project bind failed for project: " + project.getName(), e);
					return new Status(IStatus.ERROR, MicroclimateUIPlugin.PLUGIN_ID,
							"An error occurred trying to bind the " + project.getName() + " project.", e);
				}
			}
		};
		job.schedule();
		return true;
	}
}
