package com.ibm.microclimate.core.internal;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * General utils that don't belong anywhere else
 *
 * @author timetchells@ibm.com
 *
 */
public class Util {

	/**
	 * Open a dialog on top of the current active window. Can be called off the UI thread.
	 */
	public static void openDialog(boolean isError, String title, String msg) {
		final int kind = isError ? MessageDialog.ERROR : MessageDialog.INFORMATION;

		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.open(kind, Display.getDefault().getActiveShell(), title, msg, 0);
			}
		});
	}
}
