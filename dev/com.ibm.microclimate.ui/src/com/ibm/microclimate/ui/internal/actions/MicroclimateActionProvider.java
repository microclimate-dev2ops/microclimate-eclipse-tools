/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

/**
 * Action provider for the Microclimate view.
 */
public class MicroclimateActionProvider extends CommonActionProvider {
	
	private CreateConnectionAction createConnectionAction;
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        Shell shell = aSite.getViewSite().getShell();
        createConnectionAction = new CreateConnectionAction(shell);
    }
    
    @Override
    public void fillContextMenu(IMenuManager menu) {
    	menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, createConnectionAction);
    }

}
