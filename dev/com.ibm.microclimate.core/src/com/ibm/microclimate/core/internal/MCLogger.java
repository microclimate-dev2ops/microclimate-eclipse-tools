package com.ibm.microclimate.core.internal;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

import com.ibm.microclimate.core.Activator;

/**
 * @author timetchells@ibm.com
 */
public class MCLogger implements DebugOptionsListener {

	// private static final SimpleDateFormat TIME_SDF = new SimpleDateFormat("k:mm:ss");

	private static final ILog logger = com.ibm.microclimate.core.Activator.getDefault().getLog();

	private static MCLogger instance;

	private MCLogger() {
		instance = this;
	}

	/**
	 * This should only be used by the Activator. Call this class's methods statically.
	 * @return
	 */
	public static MCLogger instance() {
		if (instance == null) {
			instance = new MCLogger();
		}
		return instance;
	}

	private static boolean
			logInfo;

	private static final String
			INFO_LEVEL = "/debug/info";

	@Override
	public void optionsChanged(DebugOptions debugOptions) {
		// Note that we have to register this class with the debug options service in Activator.start

		// Always log errors.
		// logError = debugOptions.getBooleanOption(com.ibm.microclimate.core.Activator.PLUGIN_ID + ERROR_LEVEL, true);

		// Toggle info logging by creating a .options file in your Eclipse installation directory with this content:
		// com.ibm.microclimate.core/debug/info=true
		// and then passing eclipse the '-debug' option

		logInfo = debugOptions.getBooleanOption(com.ibm.microclimate.core.Activator.PLUGIN_ID + INFO_LEVEL, false);
	}

	public static void log(String msg) {
		writeLog(msg, false, null);
	}

	public static void logError(String msg) {
		writeLog(msg, true, null);
	}

	public static void logError(Throwable t) {
		logError("Exception occurred:", t);
	}

	public static void logError(String msg, Throwable t) {
		writeLog(msg, true, t);
	}

	/**
	 * Log the given message to stdout or stderr, depending on isError.
	 * The message is prepended with a timestamp, as well as the caller's class name, method name, and line number.
	 */
	private static void writeLog(String msg, boolean isError, Throwable t) {
		if (!isError && !logInfo) {
			// Not logging info at this time; do nothing.
			return;
		}

		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StackTraceElement callingMethod = null;
		for (int x = 0; x < ste.length; x++) {
			if (ste[x].getMethodName().equals("writeLog")) {
				callingMethod = ste[x++];
				// Skip over logging methods, we want to print their callers.
				while (callingMethod.getMethodName().equals("writeLog")
						|| callingMethod.getMethodName().equals("log")
						|| callingMethod.getMethodName().equals("logError")) {
					callingMethod = ste[x++];
				}
				break;
			}
		}

		// String time = TIME_SDF.format(Calendar.getInstance().getTime());

		String callerInfo = "unknown";

		if (callingMethod != null) {
			String className = callingMethod.getClassName();
			String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

			callerInfo = String.format("%s.%s:%s",
					simpleClassName, callingMethod.getMethodName(), callingMethod.getLineNumber());
		}

		String type = isError ? "ERROR" : "INFO";
		String fullMessage = String.format("[%s %s] %s", type, callerInfo, msg);

		int level = isError ? IStatus.ERROR : IStatus.INFO;
		IStatus status;

		if (t != null) {
			status = new Status(level, Activator.PLUGIN_ID, fullMessage, t);
		}
		else {
			status = new Status(level, Activator.PLUGIN_ID, fullMessage);
		}

		logger.log(status);
	}
}
