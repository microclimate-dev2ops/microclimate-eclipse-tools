/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.ui.console.IConsole;

import com.ibm.microclimate.core.internal.console.MicroclimateConsoleFactory;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action for toggling the display of a console showing the build logs
 * for a Microclimate application.
 */
public class ToggleBuildConsoleAction extends ToggleConsoleAction {
	
	public ToggleBuildConsoleAction() {
		super(Messages.ShowBuildConsoleAction);
    }
	
	@Override 
	public boolean consoleSupported() {
		return app.hasBuildLog();
	}

	@Override
	protected IConsole createConsole() {
		return MicroclimateConsoleFactory.createBuildConsole(app);
	}

	@Override
	protected void setConsole(IConsole console) {
		app.setBuildConsole(console);
	}

	@Override
	protected boolean hasConsole() {
		return app.hasBuildConsole();
	}

	@Override
	protected IConsole getConsole() {
		return app.getBuildConsole();
	}

}
