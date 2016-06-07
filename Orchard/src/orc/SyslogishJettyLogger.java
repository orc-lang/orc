//
// SyslogishJettyLogger.java -- Java class SyslogishJettyLogger
// Project Orchard
//
// Created by jthywiss on Apr 27, 2012.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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
import java.util.logging.LogManager;

import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Jetty Logger that uses a syslog-inspired format
 *
 * @author jthywiss
 */
public class SyslogishJettyLogger extends AbstractLogger implements Logger {

    private final String loggerName;
    private int currentLevel = LEVEL_INFO;
    private static String lineSeparator = System.getProperty("line.separator");
    private final Calendar timestamp = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.ROOT);

    public SyslogishJettyLogger() {
        this(null);
    }

    public SyslogishJettyLogger(final String name) {
        loggerName = name == null ? "" : name;

        initLoggingLevel();
    }

    protected void initLoggingLevel() {
        int level = lookupLoggingLevel(this.loggerName);
        if (level == LEVEL_DEFAULT) {
            level = lookupLoggingLevel("log");
            if (level == LEVEL_DEFAULT) {
                level = LEVEL_INFO;
            }
        }
    }

    // Derived from org.eclipse.jetty.util.log.AbstractLogger
    public static int lookupLoggingLevel(final String name) {
        // Calculate the level this named logger should operate under.
        // Checking with FQCN first, then each package segment from longest to
        // shortest.
        String nameSegment = name;

        while (nameSegment != null && nameSegment.length() > 0) {
            final String levelStr = LogManager.getLogManager().getProperty(nameSegment + ".LEVEL");
            // System.err.printf("[StdErrLog.CONFIG] Checking for property [%s.LEVEL] = %s%n",nameSegment,levelStr);
            final int level = getLevelId(nameSegment + ".LEVEL", levelStr);
            if (level != -1) {
                return level;
            }

            // Trim and try again.
            final int idx = nameSegment.lastIndexOf('.');
            if (idx >= 0) {
                nameSegment = nameSegment.substring(0, idx);
            } else {
                nameSegment = null;
            }
        }

        // Default Logging Level
        return LEVEL_DEFAULT;
    }

    @Override
    protected Logger newLogger(final String fullname) {
        final SyslogishJettyLogger logger = new SyslogishJettyLogger(fullname);

        logger.currentLevel = currentLevel;

        return logger;
    }

    @Override
    public String getName() {
        return loggerName;
    }

    @Override
    public String toString() {
        return loggerName.isEmpty() ? "(root logger)" : loggerName;
    }

    @Override
    public boolean isDebugEnabled() {
        return currentLevel <= LEVEL_DEBUG;
    }

    @Override
    public void setDebugEnabled(final boolean enabled) {
        if (enabled) {
            currentLevel = LEVEL_DEBUG;
        } else {
            initLoggingLevel();
        }
    }

    @Override
    public void warn(final String msg, final Throwable thrown) {
        if (currentLevel <= LEVEL_WARN) {
            if (thrown instanceof RuntimeException || thrown instanceof Error) {
                publish(Level.SEVERE, msg, thrown);
            } else {
                publish(Level.WARNING, msg, thrown);
            }
        }
    }

    @Override
    public void warn(final Throwable thrown) {
        warn(Log.EXCEPTION, thrown);
    }

    @Override
    public void warn(final String msg, final Object... args) {
        if (currentLevel <= LEVEL_WARN) {
            publish(Level.WARNING, msg, null, args);
        }
    }

    @Override
    public void info(final String msg, final Object... args) {
        if (currentLevel <= LEVEL_INFO) {
            publish(Level.INFO, msg, null, args);
        }
    }

    @Override
    public void info(final Throwable thrown) {
        info(Log.EXCEPTION, thrown);
    }

    @Override
    public void info(final String msg, final Throwable thrown) {
        if (currentLevel <= LEVEL_INFO) {
            publish(Level.INFO, msg, thrown);
        }
    }

    @Override
    public void debug(final String msg, final Object... args) {
        if (currentLevel <= LEVEL_DEBUG) {
            publish(Level.FINER, msg, null, args);
        }
    }

    @Override
    public void debug(final String msg, final long arg) {
        if (currentLevel <= LEVEL_DEBUG) {
            publish(Level.FINER, msg, null, arg);
        }
    }

    @Override
    public void debug(final Throwable thrown) {
        debug(Log.EXCEPTION, thrown);
    }

    @Override
    public void debug(final String msg, final Throwable thrown) {
        if (currentLevel <= LEVEL_DEBUG) {
            publish(Level.FINER, msg, thrown);
        }
    }

    @Override
    public void ignore(final Throwable thrown) {
        debug(Log.IGNORED, thrown);
    }

    protected void publish(final Level level, final String message, final Throwable thrown, final Object... args) {
        System.err.print(format(level, message, thrown, args));
    }

    protected String format(final Level level, final String message, final Throwable thrown, final Object... args) {
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

        formatAndAppendMessage(sb, message, args);
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
            if (cname.equals(getClass().getName()) || cname.startsWith("org.eclipse.jetty.util.log.")) {
                break;
            }
            ix++;
        }
        // Now search for the first frame before the "Logger" class.
        while (ix < stack.length) {
            final StackTraceElement frame = stack[ix];
            final String cname = frame.getClassName();
            if (!cname.equals(getClass().getName()) && !cname.startsWith("org.eclipse.jetty.util.log.")) {
                // We've found the relevant frame.
                return frame;
            }
            ix++;
        }
        return null;
    }

    // Derived from org.eclipse.jetty.util.log.StdErrLog
    private void formatAndAppendMessage(final StringBuffer sb, final String message, final Object... args) {
        final String safeMessage = String.valueOf(message);
        int messagePos = 0;
        for (final Object arg : args) {
            final int bracesIndex = safeMessage.indexOf("{}", messagePos);
            if (bracesIndex < 0) {
                sb.append(safeMessage.substring(messagePos));
                sb.append(' ');
                sb.append(String.valueOf(arg));
                messagePos = safeMessage.length();
            } else {
                sb.append(safeMessage.substring(messagePos, bracesIndex));
                sb.append(String.valueOf(arg));
                messagePos = bracesIndex + 2;
            }
        }
    }

}
