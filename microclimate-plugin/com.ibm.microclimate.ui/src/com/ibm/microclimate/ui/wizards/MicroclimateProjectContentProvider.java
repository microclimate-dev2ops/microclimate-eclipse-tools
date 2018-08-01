package com.ibm.microclimate.ui.wizards;

import org.eclipse.jface.viewers.ITreeContentProvider;

public class MicroclimateProjectContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object parent) {
		return new String[] { "one", "two" };
	}

	@Override
	public Object[] getElements(Object obj) {
		// TODO Auto-generated method stub
		return getChildren(obj);
	}

	@Override
	public Object getParent(Object arg0) {
		return "Parent";
	}

	@Override
	public boolean hasChildren(Object parent) {
		return getChildren(parent).length > 0;
	}

}