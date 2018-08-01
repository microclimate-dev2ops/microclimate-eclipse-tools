package com.ibm.microclimate.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.ibm.microclimate.ui.Activator;

public class LinkMicroclimateProjectWizard extends Wizard implements INewWizard {
	
	private IProject project;
	
	private LinkMicroclimateProjectPage newProjectPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		project = getProjectFromSelection(selection);		
		setDefaultPageImageDescriptor(Activator.getDefaultIcon());	
		
		// TODO help
		setHelpAvailable(false);
	}
	
	
	private static IProject getProjectFromSelection(IStructuredSelection selection) {
		if (selection == null) {
			System.err.println("Null selection passed to getProjectFromSelection");
			return null;
		}
		
		IProject project = ProjectUtilities.getProject(selection.getFirstElement());
		if (project == null){
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IResource){
				project = ((IResource)firstElement).getProject();
			}
		}
		// If there are criteria which exclude certain projects, check those here, 
		// and return null if the project is not valid
		return project;
	}
	
	
	@Override
	public void addPages() {
		setWindowTitle("Link Microclimate Project Wizard Window Title");

		newProjectPage = new LinkMicroclimateProjectPage();
		// newConnectionPage.setProject(project);
		addPage(newProjectPage);
	}
	
	@Override
	public boolean canFinish() {
		return newProjectPage.canFinish();
	}
	
	@Override
	public boolean performFinish() {
		MessageDialog.openInformation(getShell(), "Linking Complete",
				"Linked project " + project.getName() + " with MC project " + newProjectPage.getSelectedApp().name());
		return true;
	}
}
