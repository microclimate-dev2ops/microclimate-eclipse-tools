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
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

import com.ibm.microclimate.core.internal.MCEclipseApplication;
import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.console.ProjectLogInfo;
import com.ibm.microclimate.core.internal.console.SocketConsole;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Hide all log files action
 */
public class HideAllLogsAction extends Action {

    protected MCEclipseApplication app;
    
    public HideAllLogsAction() {
    	super(Messages.HideAllLogFilesAction);
    }

    public void setApp(MCEclipseApplication app) {
        this.app = app;

    	// Only enable if there is at least one log file that has a console
        boolean enabled = false;
        if (app.getLogInfos() != null && !app.getLogInfos().isEmpty()) {
	    	for (ProjectLogInfo logInfo : app.getLogInfos()) {
	    		if (app.getConsole(logInfo) != null) {
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
        	MCLogger.logError("HideAllLogsAction ran but no Microclimate application was selected");
			return;
		}
        
        if (app.getLogInfos() == null || app.getLogInfos().isEmpty()) {
        	MCLogger.logError("HideAllLogsAction ran but there are no logs for the selected application: " + app.name);
        	return;
        }
        
        // Remove any existing consoles for this app
        for (ProjectLogInfo logInfo : app.getLogInfos()) {
        	SocketConsole console = app.getConsole(logInfo);
    		if (console != null) {
				IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
				consoleManager.removeConsoles(new IConsole[] { console });
				app.removeConsole(console);
    		}
    	}
    }
}
