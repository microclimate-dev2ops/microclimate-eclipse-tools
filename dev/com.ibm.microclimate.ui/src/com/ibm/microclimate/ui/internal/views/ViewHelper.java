/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.views;

import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

import com.ibm.microclimate.core.internal.MCLogger;
import com.ibm.microclimate.core.internal.MicroclimateApplication;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;

/**
 * Utilities for refreshing the Microclimate view.
 */
public class ViewHelper {
	
	public static void openMicroclimateExplorerView() {
		openNavigatorView(MicroclimateExplorerView.VIEW_ID);
	}
	
	public static void refreshMicroclimateExplorerView(Object element) {
		final Object obj = element == null ? ResourcesPlugin.getWorkspace().getRoot() : element;
		Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
            	refreshNavigatorView(MicroclimateExplorerView.VIEW_ID, obj);
            }
        });
	}
	
	public static void expandConnection(MicroclimateConnection connection) {
		if (connection == null) {
			return;
		}
		List<MicroclimateApplication> apps = connection.getApps();
		if (!apps.isEmpty()) {
			IViewPart view = getViewPart(MicroclimateExplorerView.VIEW_ID);
			if (view instanceof CommonNavigator) {
				CommonViewer viewer = ((CommonNavigator)view).getCommonViewer();
				viewer.expandToLevel(2);
 			}
		}
	}
	
    public static void openNavigatorView(String viewId) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IWorkbenchPart part = page.findView(viewId);
                if (part == null) {
                    try {
                        part = page.showView(viewId);
                    } catch (PartInitException e) {
                        MCLogger.logError("An error occurred when trying to open the navigator view: " + viewId, e);
                    }
                }
            }
        }
    }

    public static void refreshNavigatorView(String viewId, Object element) {
    	IViewPart part = getViewPart(viewId);
        if (part != null) {
            if (part instanceof CommonNavigator) {
                CommonNavigator v = (CommonNavigator) part;
                v.getCommonViewer().refresh(element);
            }
        }
    }
    
	public static IViewPart getViewPart(String viewId) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                return page.findView(viewId);
            }
        }
        return null;
	}
	
}
