package com.ibm.microclimate.core.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;

import com.ibm.microclimate.core.Activator;

public class MicroclimateServerBehaviour extends ServerBehaviourDelegate {

	@Override
	public void stop(boolean arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public IStatus canPublish() {
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Microclimate server does not support publish", null);
	}

	@Override
	public void restart(String launchMode) throws CoreException {
		// TODO Auto-generated method stub
	}

}
