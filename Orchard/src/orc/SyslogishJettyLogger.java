//
// SyslogishJettyLogger.java -- Java class SyslogishJettyLogger
// Project Orchard
//
// $Id$
//
// Created by jthywiss on Apr 27, 2012.
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;

import org.mortbay.log.Logger;

/**
 * Jetty Logger that uses a syslog-inspired format
 *
 * @author jthywiss
 */
public class SyslogishJettyLogger implements Logger {

	private final String loggerName;
	private boolean debugEnabled = false;
	private static String lineSeparator = System.getProperty("line.separator");
	private final Calendar timestamp = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.ROOT);

	public SyslogishJettyLogger() {
		this(null);
	}

	public SyslogishJettyLogger(final String name) {
		loggerName = name == null ? "" : name;
	}

	/* (non-Javadoc)
	 * @see org.mortbay.log.Logger#getLogger(java.lang.String)
	 */
	@Override
	public Logger getLogger(final String name) {
		if ((name == null || name.isEmpty()) && this.loggerName == null || name != null && name.equals(this.loggerName)) {
			return this;
		}
		return new SyslogishJettyLogger(name);
	}

	/* (non-Javadoc)
	 * @see org.mortbay.log.Logger#isDebugEnabled()
	 */
	@Override
	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	/* (non-Javadoc)
	 * @see org.mortbay.log.Logger#setDebugEnabled(boolean)
	 */
	@Override
	public void setDebugEnabled(final boolean enabled) {
		debugEnabled = enabled;
	}

	/* (non-Javadoc)
	 * @see org.mortbay.log.Logger#warn(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void warn(final String msg, final Throwable thrown) {
		if (thrown instanceof RuntimeException || thrown instanceof Error) {
			publish(Level.SEVERE, msg, null, null, thrown);
		} else {
			publish(Level.WARNING, msg, null, null, thrown);
		}
	}

	/* (non-Javadoc)
	 * @see org.mortbay.log.Logger#warn(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void warn(final String msg, final Object arg0, final Object arg1) {
		publish(Level.WARNING, msg, arg0, arg1, null);
	}

	/* (non-Javadoc)
	 * @see org.mortbay.log.Logger#info(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void info(final String msg, final Object arg0, final Object arg1) {
		publish(Level.INFO, msg, arg0, arg1, null);
	}

	/* (non-Javadoc)
	 * @see org.mortbay.log.Logger#debug(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void debug(final String msg, final Throwable thrown) {
		if (debugEnabled) {
			publish(Level.FINER, msg, null, null, thrown);
		}
	}

	/* (non-Javadoc)
	 * @see org.mortbay.log.Logger#debug(java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void debug(final String msg, final Object arg0, final Object arg1) {
		if (debugEnabled) {
			publish(Level.FINER, msg, arg0, arg1, null);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return loggerName.isEmpty() ? "(root logger)" : loggerName;
	}

	protected void publish(final Level level, final String message, final Object arg0, final Object arg1, final Throwable thrown) {
		System.err.print(format(level, message, arg0, arg1, thrown));
	}

	protected String format(final Level level, final String message, final Object arg0, final Object arg1, final Throwable thrown) {
		// Log line format:
		// dateTtimeZ app class method [thread]: level: message
		// possibly followed by stack trace

		final StringBuffer sb = new StringBuffer();

		timestamp.setTimeInMillis(System.currentTimeMillis());
		sb.append(timestamp.get(Calendar.YEAR));
		sb.append('-');
		if (timestamp.get(Calendar.MONTH) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.MONTH));
		sb.append('-');
		if (timestamp.get(Calendar.DAY_OF_MONTH) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.DAY_OF_MONTH));
		sb.append('T');
		if (timestamp.get(Calendar.HOUR_OF_DAY) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.HOUR_OF_DAY));
		sb.append(':');
		if (timestamp.get(Calendar.MINUTE) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.MINUTE));
		sb.append(':');
		if (timestamp.get(Calendar.SECOND) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.SECOND));
		sb.append('.');
		if (timestamp.get(Calendar.MILLISECOND) < 100) {
			sb.append('0');
		}
		if (timestamp.get(Calendar.MILLISECOND) < 10) {
			sb.append('0');
		}
		sb.append(timestamp.get(Calendar.MILLISECOND));
		sb.append("Z ");

		if (loggerName != null && !loggerName.isEmpty()) {
			sb.append(loggerName);
		} else {
			sb.append('-');
		}
		sb.append(' ');

		final StackTraceElement caller = inferCaller();

		if (caller != null && !caller.getClassName().isEmpty()) {
			sb.append(caller.getClassName());
		} else {
			sb.append('-');
		}
		sb.append(' ');

		if (caller != null && !caller.getMethodName().isEmpty()) {
			sb.append(caller.getMethodName());
		} else {
			sb.append('-');
		}

		sb.append(" [");
		sb.append(Thread.currentThread().getName());
		sb.append("]: ");

		sb.append(level.getLocalizedName());
		sb.append(": ");

		formatAndAppendMessage(sb, message, arg0, arg1);
		sb.append(lineSeparator);

		if (thrown != null) {
			try {
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw);
				thrown.printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (final Exception ex) {
			}
		}
		return sb.toString();
	}

	// Derived from java.util.logging.LogRecord
	private StackTraceElement inferCaller() {
		// Get the stack trace.
		final StackTraceElement stack[] = new Throwable().getStackTrace();
		// First, search back to a method in the Logger class.
		int ix = 0;
		while (ix < stack.length) {
			final StackTraceElement frame = stack[ix];
			final String cname = frame.getClassName();
			if (cname.equals(getClass().getName()) || cname.startsWith("org.mortbay.log.")) {
				break;
			}
			ix++;
		}
		// Now search for the first frame before the "Logger" class.
		while (ix < stack.length) {
			final StackTraceElement frame = stack[ix];
			final String cname = frame.getClassName();
			if (!cname.equals(getClass().getName()) && !cname.startsWith("org.mortbay.log.")) {
				// We've found the relevant frame.
				return frame;
			}
			ix++;
		}
		return null;
	}

	// Derived from org.mortbay.log.StdErrLog
	private void formatAndAppendMessage(final StringBuffer sb, final String message, final Object arg0, final Object arg1) {
		final int i0 = message.indexOf("{}");
		final int i1 = i0 < 0 ? -1 : message.indexOf("{}", i0 + 2);

		if (i0 >= 0) {
			sb.append(message.substring(0, i0));
			sb.append(String.valueOf(arg0));

			if (i1 >= 0) {
				sb.append(message.substring(i0 + 2, i1));
				sb.append(String.valueOf(arg1));
				sb.append(message.substring(i1 + 2));
			} else {
				sb.append(message.substring(i0 + 2));
				if (arg1 != null) {
					sb.append(' ');
					sb.append(String.valueOf(arg1));
				}
			}
		} else {
			sb.append(message);
			if (arg0 != null) {
				sb.append(' ');
				sb.append(String.valueOf(arg0));
			}
			if (arg1 != null) {
				sb.append(' ');
				sb.append(String.valueOf(arg1));
			}
		}
	}

}
