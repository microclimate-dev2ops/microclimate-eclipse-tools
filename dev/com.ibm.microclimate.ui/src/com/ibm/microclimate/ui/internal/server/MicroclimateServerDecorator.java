package com.ibm.microclimate.ui.internal.server;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.wst.server.core.IServer;

import com.ibm.microclimate.core.internal.server.MicroclimateServer;
import com.ibm.microclimate.core.internal.server.MicroclimateServerBehaviour;
import com.ibm.microclimate.ui.Activator;

/**
 * From com.ibm.ws.st.ui.internal.ServerDecorator
 */
public class MicroclimateServerDecorator extends LabelProvider implements ILightweightLabelDecorator {

	/*
	// org.eclipse.cft.server.ui.internal.CloudFoundryDecorator
	private final MicroclimateServerListener listener;

	public MicroclimateServerDecorator() {
		this.listener = new MicroclimateServerListener() {
			@Override
			public void serverChanged(final String event) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						LabelProviderChangedEvent labelEvent =
								new LabelProviderChangedEvent(MicroclimateServerDecorator.this);
						fireLabelProviderChanged(labelEvent);
					}
				});
			}
		};
	}*/

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

            if (mcServer != null) {
            	String err = mcServer.getSuffix();
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
