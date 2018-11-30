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

/**
 * Action for toggling the display of a console showing the application logs
 * for a Microclimate application.
 */
public class ToggleAppConsoleAction extends ToggleConsoleAction {

	@Override
	protected boolean supportsConsole() {
		return true;
	}
	
	@Override
	protected IConsole createConsole() {
		return MicroclimateConsoleFactory.createApplicationConsole(app);
	}

	@Override
	protected void setConsole(IConsole console) {
		app.setAppConsole(console);
	}

	@Override
	protected boolean hasConsole() {
		return app.hasAppConsole();
	}

	@Override
	protected IConsole getConsole() {
		return app.getAppConsole();
	}

}