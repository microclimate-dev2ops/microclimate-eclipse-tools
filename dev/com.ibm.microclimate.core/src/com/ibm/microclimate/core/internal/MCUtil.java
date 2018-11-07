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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.ibm.microclimate.core.MicroclimateCorePlugin;
import com.ibm.microclimate.core.internal.connection.MicroclimateConnection;

/**
 * General utils that don't belong anywhere else
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
		s.useDelimiter("\\A"); //$NON-NLS-1$
		String result = s.hasNext() ? s.next() : ""; //$NON-NLS-1$
		s.close();
		return result;
	}

	public static boolean isWindows() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("windows");
	}

	public static boolean pathEquals(IPath path, IPath path2) {
		if (isWindows()) {
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
	 * Splits the given Path into strings with a maximum length of the given length.
	 * Splits only across separators, so individual path segments will remain intact.
	 */
	public static List<String> splitPath(IPath path, int length) {
		List<String> result = new ArrayList<>();

		StringBuilder currentString = new StringBuilder().append(IPath.SEPARATOR);
		for (String segment : path.segments()) {
			if (currentString.length() + segment.length() > length) {
				result.add(currentString.toString());
				currentString = new StringBuilder();
			}
			currentString.append(segment).append(IPath.SEPARATOR);
		}

		if (currentString.length() > 0) {
			result.add(currentString.toString());
		}

		return result;
	}

	/**
	 * In: [ "Here", "Is", "Some Input" ]
	 * Out: "Here, Is, Some Input"
	 */
	public static String toCommaSeparatedString(Collection<String> collection) {
		StringBuilder resultBuilder = new StringBuilder();

		final String separator = ", "; //$NON-NLS-1$
		for (String s : collection) {
			resultBuilder.append(s).append(separator);
		}

		// Remove the last separator
		if (resultBuilder.length() > separator.length()) {
			resultBuilder.setLength(resultBuilder.length() - separator.length());
		}

		return resultBuilder.toString();
	}
	
	public static int parsePort(String portStr) {
		try {
			return Integer.parseInt(portStr);
		}
		catch(NumberFormatException e) {
			MCLogger.logError(String.format("Couldn't parse port from \"%s\"", portStr), e); //$NON-NLS-1$
			return -1;
		}
	}
	
	public static void updateAll() {
		IUpdateHandler handler = MicroclimateCorePlugin.getUpdateHandler();
		if (handler != null) {
			handler.updateAll();
		}
	}
	
	public static void updateConnection(MicroclimateConnection connection, boolean contentChanged) {
		IUpdateHandler handler = MicroclimateCorePlugin.getUpdateHandler();
		if (handler != null) {
			handler.updateConnection(connection,  contentChanged);
		}
	}
	
	public static void updateApplication(MicroclimateApplication app, boolean contentChanged) {
		IUpdateHandler handler = MicroclimateCorePlugin.getUpdateHandler();
		if (handler != null) {
			handler.updateApplication(app,  contentChanged);
		}
	}


}
