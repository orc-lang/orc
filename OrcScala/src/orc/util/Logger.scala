//
// Logger.scala -- Scala class Logger
// Project OrcScala
//
// Created by jthywiss on Jul 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.io.{ PrintWriter, StringWriter }
import java.lang.management.ManagementFactory
import java.util.{ Calendar, GregorianCalendar, Locale, TimeZone }
import java.util.logging.{ Formatter, Level, LogRecord }

import scala.annotation.elidable

/** A Scala wrapper around java.util.logging.Logger
  * <p>
  * Sometimes, it may be unclear whether an event should be logged or sent to the user.
  * Imagine Orc running as a service, for example the "Try Orc" demo.
  * Logged messages are intended for system administrators to help determine "what is going
  * on on this server" or "why does Orc appear broken".  Events that are solely functions of end
  * user input (compile errors, etc.) should not be logged.
  * <p>
  * Levels: [from <code>java.util.Logger</code>]
  * <ul><li>
  * <strong>SEVERE</strong> is a message level indicating a serious failure.
  * In general, SEVERE messages should describe events that are of considerable importance and which will prevent normal program execution. They should be reasonably intelligible to end users and to system administrators.
  * </li><li>
  * <strong>WARNING</strong> is a message level indicating a potential problem.
  * In general, WARNING messages should describe events that will be of interest to end users or system managers, or which indicate potential problems.
  * </li><li>
  * <strong>INFO</strong> is a message level for informational messages.
  * Typically, INFO messages will be written to the console or its equivalent. So the INFO level should only be used for reasonably significant messages that will make sense to end users and system admins.
  * </li><li>
  * <strong>CONFIG</strong> is a message level for static configuration messages.
  * CONFIG messages are intended to provide a variety of static configuration information, to assist in debugging problems that may be associated with particular configurations. For example, CONFIG message might include the CPU type, the graphics depth, the GUI look-and-feel, etc.
  * </li></ul>
  * <em>Note:</em> All of the following levels, FINE, FINER, and FINEST, are intended for relatively detailed tracing. The exact meaning of the three levels will vary between subsystems, but in general, FINEST should be used for the most voluminous detailed output, FINER for somewhat less detailed output, and FINE for the lowest volume (and most important) messages.
  * <ul><li>
  * <strong>FINE</strong> is a message level providing tracing information.
  * In general, the FINE level should be used for information that will be broadly interesting to developers who do not have a specialized interest in the specific subsystem.
  * FINE messages might include things like minor (recoverable) failures. Issues indicating potential performance problems are also worth logging as FINE.
  * </li><li>
  * <strong>FINER</strong> indicates a fairly detailed tracing message. By default logging calls for entering, returning, or throwing an exception are traced at this level.
  * </li><li>
  * <strong>FINEST</strong> indicates a highly detailed tracing message.
  * </li></ul>
  * <p>
  * Differentiating usage of Logger, Tracer, and Profiler: Logging is intended
  * for abnormal or significant events.  Tracing is intended for recording
  * routine events on an object for debugging.  Profiling is intended for
  * performance measurement.
  *
  * @author jthywiss
  */
class Logger(name: String) {
  lazy val julLogger: java.util.logging.Logger = java.util.logging.Logger.getLogger(name)

  @elidable(elidable.ASSERTION) @inline final def log(level: Level, msg: => String): Unit = if (julLogger.isLoggable(level)) { val caller = getCaller(); julLogger.logp(level, caller._1, caller._2, msg) }
  //@inline final def log(level: Level, msg: => String, param1: Object): Unit = if (julLogger.isLoggable(level)) { val caller = getCaller(); julLogger.logp(level, caller._1, caller._2, msg, param1) }
  @elidable(elidable.ASSERTION) @inline final def log(level: Level, msg: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(level)) { val caller = getCaller(); julLogger.logp(level, caller._1, caller._2, msg, params.toArray) }
  @inline final def log(level: Level, msg: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(level)) { val caller = getCaller(); julLogger.logp(level, caller._1, caller._2, msg, thrown) }
  @elidable(elidable.ASSERTION) @inline final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg)
  @elidable(elidable.ASSERTION) @inline final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, param1: Object): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, param1)
  @elidable(elidable.ASSERTION) @inline final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, params.toArray)
  @inline final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, thrown)
  @elidable(elidable.ASSERTION) @inline final def entering(sourceClass: => String, sourceMethod: => String): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod)
  //@inline final def entering(sourceClass: => String, sourceMethod: => String, param1: Object): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod, param1)
  @elidable(elidable.ASSERTION) @inline final def entering(sourceClass: => String, sourceMethod: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod, params.toArray)
  @elidable(elidable.ASSERTION) @inline final def exiting(sourceClass: => String, sourceMethod: => String): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.exiting(sourceClass, sourceMethod)
  @elidable(elidable.ASSERTION) @inline final def exiting(sourceClass: => String, sourceMethod: => String, result: Object): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.exiting(sourceClass, sourceMethod, result)
  @elidable(elidable.ASSERTION) @inline final def throwing(sourceClass: => String, sourceMethod: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.throwing(sourceClass, sourceMethod, thrown)
  @inline final def severe(msg: => String): Unit = if (julLogger.isLoggable(Level.SEVERE)) { val caller = getCaller(); julLogger.logp(Level.SEVERE, caller._1, caller._2, msg) }
  @inline final def warning(msg: => String): Unit = if (julLogger.isLoggable(Level.WARNING)) { val caller = getCaller(); julLogger.logp(Level.WARNING, caller._1, caller._2, msg) }
  @elidable(elidable.ASSERTION) @inline final def info(msg: => String): Unit = if (julLogger.isLoggable(Level.INFO)) { val caller = getCaller(); julLogger.logp(Level.INFO, caller._1, caller._2, msg) }
  @elidable(elidable.ASSERTION) @inline final def config(msg: => String): Unit = if (julLogger.isLoggable(Level.CONFIG)) { val caller = getCaller(); julLogger.logp(Level.CONFIG, caller._1, caller._2, msg) }
  @elidable(elidable.ASSERTION) @inline final def fine(msg: => String): Unit = if (julLogger.isLoggable(Level.FINE)) { val caller = getCaller(); julLogger.logp(Level.FINE, caller._1, caller._2, msg) }
  @elidable(elidable.ASSERTION) @inline final def finer(msg: => String): Unit = if (julLogger.isLoggable(Level.FINER)) { val caller = getCaller(); julLogger.logp(Level.FINER, caller._1, caller._2, msg) }
  @elidable(elidable.ASSERTION) @inline final def finest(msg: => String): Unit = if (julLogger.isLoggable(Level.FINEST)) { val caller = getCaller(); julLogger.logp(Level.FINEST, caller._1, caller._2, msg) }

  @elidable(elidable.ASSERTION) @inline
  final def check(assertion: Boolean, message: => Any) {
    if (!assertion)
      log(Level.SEVERE, "Check failed.", new java.lang.Exception("check failed: " + message))
  }

  @inline private def getCaller(): (String, String) = {
    val stackTrace = new Throwable().getStackTrace
    var stackIndex = 0;
    /* First, skip down to the logging method calls */
    while (!isLoggerClassName(stackTrace(stackIndex).getClassName)) {
      stackIndex += 1
      if (stackIndex >= stackTrace.length) return (null, null)
    }
    /* Next, skip past the logging (and reflections) method calls */
    while (isLoggerClassName(stackTrace(stackIndex).getClassName) || isReflectionClassName(stackTrace(stackIndex).getClassName)) {
      stackIndex += 1
      if (stackIndex >= stackTrace.length) return (null, null)
    }
    /* Now, we're at the caller of the topmost Logging method */
    (stackTrace(stackIndex).getClassName, stackTrace(stackIndex).getMethodName)
  }

  @inline private def isLoggerClassName(className: String): Boolean = {
    className.equals("java.util.logging.Logger") ||
    className.startsWith("java.util.logging.LoggingProxyImpl") ||
    className.startsWith("sun.util.logging.") ||
    className.equals("orc.util.Logger") ||
    className.equals("orc.util.Logger$") ||
    (className.startsWith("orc.") && className.endsWith(".Logger")) ||
    (className.startsWith("orc.") && className.endsWith(".Logger$"))
  }

  @inline private def isReflectionClassName(className: String): Boolean = {
    className.startsWith("java.lang.reflect.") || className.startsWith("sun.reflect.")
  }

}

/** Log formatter that uses a syslog-inspired format.
  * Log line format:
  * {{{
  * dateTtimeZ app class method [thread]: level: message
  * }}}
  * possibly followed by stack trace
  *
  * @author jthywiss
  */
class SyslogishFormatter() extends Formatter() {

  private val appName = {
    /* Based on Sun JVM monitoring tools' heuristic */
    val commandProperty = System.getProperty("sun.java.command")
    if (commandProperty == null || commandProperty.isEmpty()) {
      "-"
    } else {
      val firstSpace = if (commandProperty.contains(" ")) commandProperty.indexOf(" ") else commandProperty.length
      val lastFileSep = commandProperty.lastIndexOf(java.io.File.separator, firstSpace - 1)
      val mainClass = commandProperty.substring(lastFileSep + 1, firstSpace)
      val lastDot = mainClass.lastIndexOf(".")
      mainClass.substring(lastDot + 1)
    }
  }

  protected val lineSeparator = System.getProperty("line.separator")
  private val timestamp = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.ROOT)

  override def format(record: LogRecord) = {
    val sb = new StringBuffer()

    timestamp.setTimeInMillis(record.getMillis())
    sb.append(timestamp.get(Calendar.YEAR))
    sb.append('-')
    if (timestamp.get(Calendar.MONTH) + 1 < 10) {
      sb.append('0')
    }
    sb.append(timestamp.get(Calendar.MONTH) + 1)
    sb.append('-')
    if (timestamp.get(Calendar.DAY_OF_MONTH) < 10) {
      sb.append('0')
    }
    sb.append(timestamp.get(Calendar.DAY_OF_MONTH))
    sb.append('T')
    if (timestamp.get(Calendar.HOUR_OF_DAY) < 10) {
      sb.append('0')
    }
    sb.append(timestamp.get(Calendar.HOUR_OF_DAY))
    sb.append(':')
    if (timestamp.get(Calendar.MINUTE) < 10) {
      sb.append('0')
    }
    sb.append(timestamp.get(Calendar.MINUTE))
    sb.append(':')
    if (timestamp.get(Calendar.SECOND) < 10) {
      sb.append('0')
    }
    sb.append(timestamp.get(Calendar.SECOND))
    sb.append('.')
    if (timestamp.get(Calendar.MILLISECOND) < 100) {
      sb.append('0')
    }
    if (timestamp.get(Calendar.MILLISECOND) < 10) {
      sb.append('0')
    }
    sb.append(timestamp.get(Calendar.MILLISECOND))
    sb.append("Z ")

    sb.append(appName)
    sb.append(' ')

    if (record.getSourceClassName() != null && !record.getSourceClassName().isEmpty()) {
      sb.append(record.getSourceClassName())
    } else {
      /* If no stack trace, assume the logger name is the class name */
      if (record.getLoggerName() != null && !record.getLoggerName().isEmpty()) {
        sb.append(record.getLoggerName())
      } else {
        sb.append('-')
      }
    }
    sb.append(' ')

    if (record.getSourceMethodName() != null && !record.getSourceMethodName().isEmpty()) {
      sb.append(record.getSourceMethodName())
    } else {
      sb.append('-')
    }

    sb.append(" [vm ")
    sb.append(ManagementFactory.getRuntimeMXBean().getName())
    sb.append(", thread ")
    sb.append(record.getThreadID())
    sb.append("]: ")

    sb.append(record.getLevel().getLocalizedName())
    sb.append(": ")

    sb.append(formatMessage(record))
    sb.append(lineSeparator)

    if (record.getThrown() != null) {
      try {
        val sw = new StringWriter()
        val pw = new PrintWriter(sw)
        record.getThrown().printStackTrace(pw)
        pw.close()
        sb.append(sw.toString())
      } catch {
        case ex: Exception => { /* ignore */ }
      }
    }
    sb.toString()
  }

}
