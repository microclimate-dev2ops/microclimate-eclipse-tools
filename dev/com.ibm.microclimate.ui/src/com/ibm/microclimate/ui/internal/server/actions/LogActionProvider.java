/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.server.actions;

/**
 * From com.ibm.ws.st.ui.internal.actions.LogActionProvider
 *
 */
/*
 */
// Commented out for 18.09
/*
public class LogActionProvider extends CommonActionProvider implements ISelectionChangedListener {

    protected MicroclimateServerBehaviour mcServerBehaviour;
    protected Shell shell;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        shell = aSite.getViewSite().getShell();

        ISelectionProvider selectionProvider = aSite.getStructuredViewer();
        selectionProvider.addSelectionChangedListener(this);
        onSelectionChange(selectionProvider.getSelection());
    }

	@Override
	public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
		onSelectionChange(selectionChangedEvent.getSelection());
	}

	private void onSelectionChange(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;

	        Iterator<?> iterator = sel.iterator();
	        while (iterator.hasNext()) {
	            Object obj = iterator.next();
	            if (obj instanceof IServer) {
	                IServer server = (IServer) obj;
	                mcServerBehaviour = (MicroclimateServerBehaviour)
	                		server.loadAdapter(MicroclimateServerBehaviour.class, null);
	            }
	        }
		}
	}

    @Override
    public void fillContextMenu(IMenuManager menu) {

    	if (mcServerBehaviour == null) {
    		return;
    	}


        MenuManager openLogsMenu = new MenuManager(Messages.LogActionProvider_OpenLogFileCategory, "OpenLogFiles");

    	for (IPath logFilePath : mcServerBehaviour.getApp().getLogFilePaths()) {
    		String name = logFilePath.lastSegment();
			if (name.endsWith(MCConstants.BUILD_LOG_SHORTNAME)) {
				name = MCConstants.BUILD_LOG_SHORTNAME;
			}

			OpenLogAction openLogAction = new OpenLogAction(Messages.LogActionProvider_OpenLogMenuBtn + name, shell, logFilePath);
			openLogsMenu.add(openLogAction);
    	}

        menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, openLogsMenu);

    }
}*/