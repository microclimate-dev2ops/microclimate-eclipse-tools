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
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.constants.MCConstants;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * Action to open the application overview in a browser.
 */
public class OpenAppOverviewAction implements IObjectActionDelegate {

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
            	app = (MicroclimateApplication) obj;
            	action.setEnabled(true);
            	return;
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	MCLogger.logError("OpenAppOverviewAction ran but no Microclimate application was selected");
			return;
		}

        URL url = app.mcConnection.getAppOverviewURL(app);
        if (url == null) {
        	// this should not happen
        	MCLogger.logError("OpenAppOverviewAction ran but the url was null");
			return;
        }

        // Use the app's ID as the browser ID so that if this is called again on the same app,
        // the browser will be re-used

		try {
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser = browserSupport
					.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
							app.projectID + "_" + MCConstants.VIEW_OVERVIEW, app.name, NLS.bind(Messages.BrowserTooltipAppOverview, app.name));

	        browser.openURL(url);
		} catch (PartInitException e) {
			MCLogger.logError("Error opening the app overview in browser", e); //$NON-NLS-1$
		}
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
