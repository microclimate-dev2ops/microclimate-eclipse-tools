package com.ibm.microclimate.ui;

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
 * A tab group for launching the server.
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
        tabs[1] = new SourceLookupTab();
        tabs[1].setLaunchConfigurationDialog(dialog);
        tabs[2] = new CommonTab();
        tabs[2].setLaunchConfigurationDialog(dialog);
        setTabs(tabs);
    }
}
