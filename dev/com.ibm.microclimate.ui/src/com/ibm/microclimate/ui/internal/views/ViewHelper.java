/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.views;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

import com.ibm.microclimate.core.internal.MCLogger;

public class ViewHelper {
	
	public static final String MC_EXPLORER_VIEW_ID = "com.ibm.microclimate.ui.explorerView";
	
	public static void openMicroclimateExplorerView() {
		openNavigatorView(MC_EXPLORER_VIEW_ID);
	}
	
	public static void refreshMicroclimateExplorerView(Object element) {
		final Object obj = element == null ? ResourcesPlugin.getWorkspace().getRoot() : element;
		Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
            	refreshNavigatorView(MC_EXPLORER_VIEW_ID, obj);
            }
        });
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
    	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IWorkbenchPart part = page.findView(viewId);
		        if (part != null) {
		            if (part instanceof CommonNavigator) {
		                CommonNavigator v = (CommonNavigator) part;
		                v.getCommonViewer().refresh(element);
		            }
		        }
            }
        }
    }
}
