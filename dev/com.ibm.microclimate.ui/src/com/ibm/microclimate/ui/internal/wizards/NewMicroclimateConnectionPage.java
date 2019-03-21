/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnectionManager;
import com.ibm.microclimate.ui.internal.SWTUtil;
import com.ibm.microclimate.ui.internal.messages.Messages;

/**
 * This simple page allows the user to add new Microclimate connections, by entering a hostname and port and
 * validating that Microclimate is indeed reachable at the given address.
 */
public class NewMicroclimateConnectionPage extends WizardPage {
	
	protected Composite outerComp = null;
	protected ConnectionComposite localComp = null;
	protected ConnectionComposite icpComp = null;
	protected ConnectionComposite activeComp = null;
	protected Point initialSize = null;

	protected NewMicroclimateConnectionPage() {
		super(Messages.NewConnectionPage_ShellTitle);
		setTitle(Messages.NewConnectionPage_WizardTitle);
		setDescription(Messages.NewConnectionPage_WizardDescription);
	}

	@Override
	public void createControl(Composite parent) {
		outerComp = new Composite(parent, SWT.NULL);
		outerComp.setLayout(new GridLayout());

		createConnectionTypes(outerComp);

		setControl(outerComp);
	}
	
	private void createConnectionTypes(Composite comp) {
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.marginWidth = 10;
        layout.horizontalSpacing = 7;
        layout.verticalSpacing = 0;
        comp.setLayout(layout);
        comp.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label label = new Label(comp, SWT.NONE);
        label.setText("Connection type:");
        label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        // Add buttons for the connection types
        Button localButton = new Button(comp, SWT.RADIO);
        localButton.setText("Local");
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        data.horizontalSpan = 2;
        localButton.setLayoutData(data);
        
        Button icpButton = new Button(comp, SWT.RADIO);
        icpButton.setText("ICP");
        data = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
        data.horizontalIndent = label.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + 7;
        data.horizontalSpan = 3;
        icpButton.setLayoutData(data);
        
        SelectionAdapter listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                Button button = (Button) e.getSource();
                if (button.getSelection()) {
                	if (activeComp != null) {
                        GridData data = (GridData) activeComp.getLayoutData();
                        if (!data.exclude) {
                            data.exclude = true;
                            activeComp.setVisible(!data.exclude);
                            activeComp.setLayoutData(data);
                        }
                    }
                    if (button == localButton) {
                    	if (localComp == null) {
                    		localComp = new LocalConnectionComposite(outerComp, NewMicroclimateConnectionPage.this);
                    	}
                    	activeComp = localComp;
                    } else {
                    	if (icpComp == null) {
                    		icpComp = new ICPConnectionComposite(outerComp, NewMicroclimateConnectionPage.this);
                    	}
                    	activeComp = icpComp;
                    }
                    updateCompLayout();
                    
                }
                outerComp.setFocus();
            }

        };
        localButton.addSelectionListener(listener);
        icpButton.addSelectionListener(listener);

        localButton.setSelection(true);
        localButton.notifyListeners(SWT.Selection, new Event());
	}
	
    protected void setupComposite(ConnectionComposite comp, boolean isIndentationRequired) {
        if (comp != null) {
            GridData data = new GridData();
            data.horizontalAlignment = SWT.FILL;
            data.grabExcessHorizontalSpace = true;
            data.verticalAlignment = SWT.FILL;
            data.widthHint = 400;
            data.grabExcessVerticalSpace = true;
            data.horizontalSpan = 3;
            data.verticalIndent = 7;
            if (isIndentationRequired)
                data.horizontalIndent = SWTUtil.convertWidthInCharsToPixels(outerComp, 4);
            comp.setLayoutData(data);
        }
    }
    
    protected void updateCompLayout() {
        // This must come after the 'activeComp' field is set since it ends up
        // invoking the isComplete method that use the 'activeComp' field.
        setupComposite(activeComp, true);
        GridData data = (GridData) activeComp.getLayoutData();
        data.exclude = false;
        activeComp.setVisible(!data.exclude);
        validate();
        outerComp.redraw();
        outerComp.layout(true, true);

        //resize wizard vertically.
        Shell shell = outerComp.getShell();
        shell.layout(true, true);

        if (initialSize == null) {
            initialSize = shell.getSize();
            initialSize.x = 800;
        }

        final Point newSize = shell.computeSize(initialSize.x, SWT.DEFAULT, true);
        shell.setSize(newSize);
    }

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}
	
    protected void validate() {
        if (activeComp != null) {
        	activeComp.validate();
        }
    }

	public void performFinish() {
		if (activeComp != null && activeComp.mcConnection != null) {
			MicroclimateConnectionManager.add(activeComp.mcConnection);
		}
	}
	
	public MicroclimateConnection getMCConnection() {
		if (activeComp != null) {
			return activeComp.mcConnection;
		}
		return null;
	}
}
