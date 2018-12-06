/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import java.net.URL;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.constants.AppState;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action to open the application in a browser.
 */
public class OpenAppAction implements IObjectActionDelegate {

    protected MicroclimateApplication app;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof MicroclimateApplication) {
            	app = (MicroclimateApplication)obj;
            	action.setEnabled(app.isAvailable() && app.getAppState() == AppState.STARTED);
            	return;
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	MCLogger.logError("OpenAppAction ran but no Microclimate application was selected");
			return;
		}

        if (!app.isRunning()) {
        	MCUtil.openDialog(true, Messages.OpenAppAction_CantOpenNotRunningAppTitle,
        			Messages.OpenAppAction_CantOpenNotRunningAppMsg);
        	return;
        }

        URL appRootUrl = app.getBaseUrl();

        // Use the app's ID as the browser ID so that if this is called again on the same app,
        // the browser will be re-used

		try {
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser = browserSupport
					.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
							app.projectID, app.name, NLS.bind(Messages.BrowserTooltipApp, app.name));

	        browser.openURL(appRootUrl);
		} catch (PartInitException e) {
			MCLogger.logError("Error opening app in browser", e); //$NON-NLS-1$
		}
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
