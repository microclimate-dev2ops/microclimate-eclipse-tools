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

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.navigator.ICommonViewerSite;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.console.MicroclimateConsoleFactory;
import com.ibm.microclimate.core.internal.console.ProjectLogInfo;
import com.ibm.microclimate.core.internal.console.SocketConsole;

/**
 * Action for showing a log file for a Microclimate application
 */
public class LogFileAction extends Action {
	
	private final ProjectLogInfo logInfo;
	private final MCEclipseApplication app;
	
	public LogFileAction(MCEclipseApplication app, ProjectLogInfo logInfo, ICommonViewerSite viewSite) {
		super(logInfo.logName, IAction.AS_CHECK_BOX);
    	this.logInfo = logInfo;
    	this.app = app;
    	setChecked(app.getConsole(logInfo) != null);
    }
	
    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	MCLogger.logError("LogFileAction ran but no Microclimate application was selected");
			return;
		}

        if (isChecked()) {
    		SocketConsole console = MicroclimateConsoleFactory.createLogFileConsole(app, logInfo);
			ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
			app.addConsole(console);
        } else {
        	SocketConsole console = app.getConsole(logInfo);
        	if (console != null) {
				IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
				consoleManager.removeConsoles(new IConsole[] { console });
				app.removeConsole(console);
        	}
        }
    }
}
