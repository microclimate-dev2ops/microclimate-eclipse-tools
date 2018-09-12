package com.ibm.microclimate.core.internal.server;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate;

public class MicroclimateModuleAdapterDelegate extends ModuleArtifactAdapterDelegate {

	// From com.ibm.etools.aries.internal.rad.ext.core.obr.modules.OBRModuleArtifactAdapterDelegate
	@Override
	public IModuleArtifact getModuleArtifact(Object object) {

		if (!(object instanceof IProject)) {
			return null;
		}

		IProject project = (IProject) object;
		final IModule module = ServerUtil.getModule(project);

		return new IModuleArtifact() {

			@Override
			public IModule getModule() {
				return module;
			}
		};

	}
}
