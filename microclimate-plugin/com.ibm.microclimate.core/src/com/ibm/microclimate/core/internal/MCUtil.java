package com.ibm.microclimate.core.internal;

import java.io.InputStream;
import java.util.Scanner;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * General utils that don't belong anywhere else
 *
 * @author timetchells@ibm.com
 *
 */
public class MCUtil {

	/**
	 * Open a dialog on top of the current active window. Can be called off the UI thread.
	 */
	public static void openDialog(boolean isError, String title, String msg) {
		final int kind = isError ? MessageDialog.ERROR : MessageDialog.INFORMATION;

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.open(kind, Display.getDefault().getActiveShell(), title, msg, 0);
			}
		});
	}


	public static String readAllFromStream(InputStream stream) {
		Scanner s = new Scanner(stream);
		// end-of-stream
		s.useDelimiter("\\A");
		String result = s.hasNext() ? s.next() : "";
		s.close();
		return result;
	}
}
