package com.ibm.microclimate.ui.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class MicroclimatePrefsParentPage extends PreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench arg0) {
		setDescription("Microclimate is really great and good");
	}

	@Override
	protected Control createContents(Composite parent) {
		// nothing to edit here
		return parent;
	}
}
