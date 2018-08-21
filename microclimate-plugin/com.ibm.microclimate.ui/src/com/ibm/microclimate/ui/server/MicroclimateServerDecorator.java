package com.ibm.microclimate.ui.server;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.server.MicroclimateServer;
import com.ibm.microclimate.core.server.MicroclimateServerBehaviour;
import com.ibm.microclimate.ui.Activator;

/**
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

            MicroclimateServerBehaviour mcServer = (MicroclimateServerBehaviour)
            		server.loadAdapter(MicroclimateServerBehaviour.class, null);

            if (mcServer != null && mcServer.getApp() != null) {
            	String err = mcServer.getApp().getErrorMsg();
            	if (err != null) {
            		ImageDescriptor img = Activator.getIcon(Activator.ERROR_ICON_PATH);
            		decoration.addOverlay(img);
            		decoration.addSuffix(" [" + err + "] ");
            	}
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
