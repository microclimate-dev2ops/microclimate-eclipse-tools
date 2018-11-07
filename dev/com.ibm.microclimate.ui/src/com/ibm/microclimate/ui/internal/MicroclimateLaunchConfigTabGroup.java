/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

/**
 * A tab group for editing a Microclimate Launch Configuration.
 * Right now there isn't really any obvious reason a user would need to use this,
 * but without it you get an error message when clicking on the launch config in the Configurations menu.
 */
public class MicroclimateLaunchConfigTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[2];

        SourceLookupTab sourceLookupTab = new SourceLookupTab();
        sourceLookupTab.setLaunchConfigurationDialog(dialog);
        tabs[0] = sourceLookupTab;

        tabs[1] = new CommonTab();
        tabs[1].setLaunchConfigurationDialog(dialog);
        setTabs(tabs);
    }
}
