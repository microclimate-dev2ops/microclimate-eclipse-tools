package com.ibm.microclimate.core.internal;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;

import com.ibm.microclimate.core.Activator;

public class MicroclimateServer extends ServerDelegate implements IURLProvider {
	
	MicroclimateApplication microclimateApplication;

	@Override
	public URL getModuleRootURL(IModule arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IStatus canModifyModules(IModule[] arg0, IModule[] arg1) {
		return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0, "Modules cannot be modified on a microclimate server", null);
	}

	@Override
	public IModule[] getChildModules(IModule[] arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IModule[] getRootModules(IModule arg0) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void modifyModules(IModule[] arg0, IModule[] arg1, IProgressMonitor arg2) throws CoreException {
		// Do nothing - not supported
	}

}
