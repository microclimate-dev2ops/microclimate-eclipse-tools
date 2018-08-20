package com.ibm.microclimate.ui.server;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.server.MicroclimateServer;

/**
 * TODO doesn't actually do anything yet.
 * From com.ibm.ws.st.ui.internal.ServerDecorator
 */
public class MicroclimateServerDecorator extends LabelProvider implements ILightweightLabelDecorator {
    @Override
    public void decorate(Object obj, IDecoration decoration) {
        if (obj instanceof IServer) {
            IServer server = (IServer) obj;
            if (server.getServerType() == null ||
            		!MicroclimateServer.SERVER_ID.equals(server.getServerType().getId())) {

                return;
            }

            MicroclimateServer mcServer = (MicroclimateServer) server.loadAdapter(MicroclimateServer.class, new NullProgressMonitor());

            if (mcServer != null) {
            	// TODO display build status here?
            	/*
                String name = mcServer.getServerDisplayName();
                if (name != null && !name.isEmpty()) {
					decoration.addSuffix(" [" + name + "]");
				}*/
            }
        }
        /*
        else if (obj instanceof IFile) {
            IFile file = (IFile) obj;
            ImageDescriptor id = Activator.getImageDescriptor(file.getName());
            if (id != null) {
				decoration.addOverlay(id);
			}
        }*/
    }
}
