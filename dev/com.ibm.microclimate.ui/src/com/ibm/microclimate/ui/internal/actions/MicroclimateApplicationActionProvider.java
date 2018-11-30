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
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

/**
 * Action provider for Microclimate applications in the Microclimate view.
 */
public class MicroclimateApplicationActionProvider extends CommonActionProvider {
	
	private ValidateAction validateAction;
	private AttachDebuggerAction attachDebuggerAction;
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        ISelectionProvider selProvider = aSite.getStructuredViewer();
        validateAction = new ValidateAction(selProvider);
        attachDebuggerAction = new AttachDebuggerAction(selProvider);
    }
    
    @Override
    public void fillContextMenu(IMenuManager menu) {
    	if (validateAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_BUILD, validateAction);
    	}
    	if (attachDebuggerAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_GENERATE, attachDebuggerAction);
    	}
    }

}