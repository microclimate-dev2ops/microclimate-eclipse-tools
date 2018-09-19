/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.ui.internal.prefs;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.ui.MicroclimateUIPlugin;
import com.ibm.microclimate.ui.internal.Messages;

/**
 * @author timetchells@ibm.com
 *
 */
public class MicroclimatePrefsParentPage extends PreferencePage implements IWorkbenchPreferencePage {

	private static IPreferenceStore prefs;

	private Button showLinkFinishDialogBtn;
	private Text debugTimeoutText;

	@Override
	public void init(IWorkbench arg0) {
		// setDescription("Expand this preferences category to set specific Microclimate preferences.");
		setImageDescriptor(MicroclimateUIPlugin.getDefaultIcon());

		prefs = MicroclimateCorePlugin.getDefault().getPreferenceStore();
	}

	@Override
	protected Control createContents(Composite parent) {
		parent.setLayout(new GridLayout(2, false));

		Label debugTimeoutLabel = new Label(parent, SWT.NONE);
		debugTimeoutLabel.setText(Messages.PrefsParentPage_DebugTimeoutLabel);

		debugTimeoutText = new Text(parent, SWT.BORDER);
		debugTimeoutText.setTextLimit(3);
		debugTimeoutText.setText("" + 	//$NON-NLS-1$
				prefs.getInt(MicroclimateCorePlugin.DEBUG_CONNECT_TIMEOUT_PREFSKEY));

		GridData debugTextData = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
		debugTextData.widthHint = 50;
		debugTimeoutText.setLayoutData(debugTextData);

		debugTimeoutText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				validate();
			}
		});


		final GridData fillRowData = new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1);
		Label spacer = new Label(parent, SWT.NONE);
		spacer.setLayoutData(fillRowData);

		showLinkFinishDialogBtn = new Button(parent, SWT.CHECK);
		showLinkFinishDialogBtn.setText(Messages.PrefsParentPage_HidePostLinkDialogLabel);
		showLinkFinishDialogBtn.setSelection(prefs.getBoolean(MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY));
//		showLinkFinishDialogBtn.setFont(parent.getFont());
		showLinkFinishDialogBtn.setLayoutData(fillRowData);


		Label endSpacer = new Label(parent, SWT.NONE);
		endSpacer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
		// this last label just moves the Default and Apply buttons over
		new Label(parent, SWT.NONE);

		return parent;
	}

	private void validate() {
		String invalidReason = null;

		String timeoutText = debugTimeoutText.getText().trim();
		boolean goodDebugTimeout = false;
		try {
			int timeout = Integer.parseInt(timeoutText);
			goodDebugTimeout = timeout > 0;
		}
		catch(NumberFormatException e) {}

		if (!goodDebugTimeout) {
			invalidReason = NLS.bind(Messages.PrefsParentPage_ErrInvalidDebugTimeout, timeoutText);
		}

		setErrorMessage(invalidReason);
		setValid(invalidReason == null);
	}

	@Override
	public boolean performOk() {
		if (!isValid()) {
			return false;
		}

		prefs.setValue(MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY, showLinkFinishDialogBtn.getSelection());

		// validate in validate() that this is a good integer
		int debugTimeout = Integer.parseInt(debugTimeoutText.getText().trim());
		prefs.setValue(MicroclimateCorePlugin.DEBUG_CONNECT_TIMEOUT_PREFSKEY, debugTimeout);

		// removes any trimmed space
		debugTimeoutText.setText("" + debugTimeout);

		return true;
	}

	@Override
	public void performDefaults() {
		showLinkFinishDialogBtn.setSelection(
				prefs.getDefaultBoolean(MicroclimateCorePlugin.HIDE_ONFINISH_MSG_PREFSKEY));

		debugTimeoutText.setText("" + 	//$NON-NLS-1$
				prefs.getDefaultInt(MicroclimateCorePlugin.DEBUG_CONNECT_TIMEOUT_PREFSKEY));
	}
}
