package com.ibm.microclimate.core;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author timetchells@ibm.com
 */
public class MCLogger {

	private static final SimpleDateFormat TIME_SDF = new SimpleDateFormat("k:mm:ss");

	private static PrintStream outStream = System.out;
	private static PrintStream errStream = System.err;

	private MCLogger() {}

	public static void log(String msg) {
		log(msg, false);
	}

	public static void logError(String msg) {
		log(msg, true);
	}

	public static void logError(Throwable t) {
		logError("Exception occurred:");
		t.printStackTrace(errStream);
	}

	public static void logError(String msg, Throwable cause) {
		logError(msg);
		cause.printStackTrace(errStream);
	}

	/**
	 * Log the given message to stdout or stderr, depending on isError.
	 * The message is prepended with a timestamp, as well as the caller's class name, method name, and line number.
	 */
	private static void log(String msg, boolean isError) {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StackTraceElement callingMethod = null;
		for (int x = 0; x < ste.length; x++) {
			if (ste[x].getMethodName().equals("log")) {
				callingMethod = ste[x++];
				// Skip over other logging methods, we want to print their callers.
				while (callingMethod.getMethodName().equals("log")
						|| callingMethod.getMethodName().equals("logError")) {
					callingMethod = ste[x++];
				}
				break;
			}
		}

		String time = TIME_SDF.format(Calendar.getInstance().getTime());

		final String level = isError ? "ERROR" : "INFO ";

		@SuppressWarnings("resource")
		final PrintStream stream = isError ? errStream : outStream;
		String callerInfo = "unknown";

		if (callingMethod != null) {
			String className = callingMethod.getClassName();
			String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

			callerInfo = String.format("%s.%s:%s",
					simpleClassName, callingMethod.getMethodName(), callingMethod.getLineNumber());
		}

		String message = String.format("[%s %s %s] %s", level, time, callerInfo, msg);

		stream.println(message);
	}
}
