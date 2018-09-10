/*******************************************************************************
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2018 All Rights Reserved.
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *******************************************************************************/

package com.ibm.microclimate.core.internal;

import java.io.InputStream;
import java.util.Collection;
import java.util.Scanner;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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

	public static boolean pathEquals(IPath path, IPath path2) {
		String os = System.getProperty("os.name");
		if (os != null && os.toLowerCase().startsWith("windows")) {
			// case-insensitivity on windows
			String pathStr = path.toOSString();
			String pathStr2 = path2.toOSString();
			return pathStr.equalsIgnoreCase(pathStr2);
		}
		return path.equals(path2);
	}

	/**
	 * Append finish to start, removing the last segment of start if it is equal to the first segment of finish.
	 */
	public static IPath appendPathWithoutDupe(IPath start, String finish) {
		IPath finishPath = new Path(finish);
		if (start.lastSegment().equals(finishPath.segment(0))) {
			start = start.removeLastSegments(1);
		}
		return start.append(finishPath);
	}

	/**
	 * Splits the given string into separate strings of the given length (except the last one).
	 */
	public static String[] splitStringIntoArray(String longString, int lineLength) {
		int noSubstrings = longString.length() / lineLength + 1;
		String[] result = new String[noSubstrings];

		int strIndex = 0;
		int i = 0;
		while (strIndex + lineLength < longString.length()) {
			String sub = longString.substring(strIndex, strIndex + lineLength - 1);
			strIndex += sub.length();
			result[i++] = sub;
		}
		// final line
		result[i] = longString.substring(strIndex);
		return result;
	}

	/**
	 * In: [ "Here", "Is", "Some Input" ]
	 * Out: "Here, Is, Some Input"
	 */
	public static String toCommaSeparatedString(Collection<String> collection) {
		StringBuilder resultBuilder = new StringBuilder();

		final String separator = ", ";
		for (String s : collection) {
			resultBuilder.append(s).append(separator);
		}

		// Remove the last separator
		if (resultBuilder.length() > separator.length()) {
			resultBuilder.setLength(resultBuilder.length() - separator.length());
		}

		return resultBuilder.toString();
	}
}
