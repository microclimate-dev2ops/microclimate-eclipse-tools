/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.ui.console.IConsole;

import com.ibm.microclimate.core.internal.console.MicroclimateConsoleFactory;

public class ToggleBuildConsoleAction extends ToggleConsoleAction {

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
