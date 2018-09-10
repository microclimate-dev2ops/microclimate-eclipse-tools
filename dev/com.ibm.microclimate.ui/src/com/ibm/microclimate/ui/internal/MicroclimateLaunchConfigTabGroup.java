/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.ServerLaunchConfigurationTab;

import com.ibm.microclimate.core.internal.server.MicroclimateServer;

/**
 * A tab group for editing a Microclimate Launch Configuration.
 * Right now there isn't really any obvious reason a user would need to use this,
 * but without it you get an error message when clicking on the launch config in the Configurations menu.
 *
 * @author timetchells@ibm.com
 */
public class MicroclimateLaunchConfigTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[3];

        IServerType[] servers = ServerCore.getServerTypes();
        List<String> list = new ArrayList<>();
        for (IServerType st : servers) {
            if (st.getId().startsWith(MicroclimateServer.SERVER_ID)) {
				list.add(st.getId());
			}
        }

        tabs[0] = new ServerLaunchConfigurationTab(list.toArray(new String[list.size()]));
        tabs[0].setLaunchConfigurationDialog(dialog);

        SourceLookupTab sourceLookupTab = new SourceLookupTab();
        sourceLookupTab.setLaunchConfigurationDialog(dialog);
        tabs[1] = sourceLookupTab;

        tabs[2] = new CommonTab();
        tabs[2].setLaunchConfigurationDialog(dialog);
        setTabs(tabs);
    }
}
