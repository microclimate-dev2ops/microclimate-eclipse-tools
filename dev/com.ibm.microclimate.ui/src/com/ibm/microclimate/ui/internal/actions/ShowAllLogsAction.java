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
import org.eclipse.ui.console.ConsolePlugin;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.console.MicroclimateConsoleFactory;
import com.ibm.microclimate.core.internal.console.ProjectLogInfo;
import com.ibm.microclimate.core.internal.console.SocketConsole;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Show all log files action
 */
public class ShowAllLogsAction extends Action {

    protected MCEclipseApplication app;
    
    public ShowAllLogsAction() {
    	super(Messages.ShowAllLogFilesAction);
    }

    public void setApp(MCEclipseApplication app) {
    	this.app = app;

    	// Only enable if there is a log file that does not currently have a console
    	boolean enabled = false;
    	if (app.getLogInfos() != null && !app.getLogInfos().isEmpty()) {
	    	for (ProjectLogInfo logInfo : app.getLogInfos()) {
	    		if (app.getConsole(logInfo) == null) {
	    			enabled = true;
	    			break;
	    		}
	    	}
    	}
    	setEnabled(enabled);
    }

    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	MCLogger.logError("ShowAllLogsAction ran but no Microclimate application was selected");
			return;
		}
        
        if (app.getLogInfos() == null || app.getLogInfos().isEmpty()) {
        	MCLogger.logError("ShowAllLogsAction ran but there are no logs for the selected application: " + app.name);
        	return;
        }
        
        // Create consoles for any log files that don't have one yet
        for (ProjectLogInfo logInfo : app.getLogInfos()) {
    		if (app.getConsole(logInfo) == null) {
    			SocketConsole console = MicroclimateConsoleFactory.createLogFileConsole(app, logInfo);
    			ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
    			app.addConsole(console);
    		}
    	}
    }
}
