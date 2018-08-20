package com.ibm.microclimate.ui.server.actions;

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

import com.ibm.microclimate.core.MCLogger;
import com.ibm.microclimate.core.server.MicroclimateServerBehaviour;

public class OpenAppAction implements IObjectActionDelegate {


    protected IServer server;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof IServer) {
                server = (IServer) obj;
                MicroclimateServerBehaviour mcServer = (MicroclimateServerBehaviour)
                		server.loadAdapter(MicroclimateServerBehaviour.class, null);

                action.setEnabled(mcServer != null);
                return;
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (server == null) {
			return;
		}

        MicroclimateServerBehaviour mcServer = (MicroclimateServerBehaviour)
        		server.loadAdapter(MicroclimateServerBehaviour.class, null);
        URL appRootUrl = mcServer.getApp().rootUrl;

        // Use the app's ID as the browser ID so that if this is called again on the same app,
        // the browser will be re-used

		try {
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser = browserSupport
					.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
							mcServer.getApp().projectID, mcServer.getApp().name, mcServer.getServer().getName());

	        browser.openURL(appRootUrl);
		} catch (PartInitException e) {
			MCLogger.logError("Error opening app in browser", e);
		}
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
