/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.server.actions;

import java.net.URL;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MCUtil;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;
import com.ibm.microclimate.ui.internal.messages.Messages;

public class OpenAppAction2 implements IObjectActionDelegate {

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
            	action.setEnabled(app.isRunning());
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
							app.projectID, app.name, "Microclimate Project - " + app.name);

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
