//
// Logger.scala -- Scala class Logger
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 10, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

import java.util.logging.Level 
import scala.annotation.elidable
import scala.annotation.elidable._
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
  *
  * @author jthywiss
  */
class Logger(name: String) {
  lazy val julLogger: java.util.logging.Logger = java.util.logging.Logger.getLogger(name)

  //TODO: LogRecord caller and method will be wrong -- need to write our own inferCaller and use
  @inline final def log(level: Level, msg: => String): Unit = if (julLogger.isLoggable(level)) julLogger.log(level, msg)
  //@inline final def log(level: Level, msg: => String, param1: Object): Unit = if (julLogger.isLoggable(level)) julLogger.log(level, msg, param1)
  @inline final def log(level: Level, msg: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(level)) julLogger.log(level, msg, params)
  @inline final def log(level: Level, msg: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(level)) julLogger.log(level, msg, thrown)
  @inline final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg)
  @inline final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, param1: Object): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, param1)
  @inline final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, params)
  @inline final def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, thrown)
  @inline final def logrb(level: Level, sourceClass: => String, sourceMethod: => String, bundleName: => String, msg: => String, param1: Object): Unit = if (julLogger.isLoggable(level)) julLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1)
  @inline final def logrb(level: Level, sourceClass: => String, sourceMethod: => String, bundleName: => String, msg: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(level)) julLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, params)
  @inline final def logrb(level: Level, sourceClass: => String, sourceMethod: => String, bundleName: => String, msg: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(level)) julLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown)
  @inline @elidable(FINER)
  final def entering(sourceClass: => String, sourceMethod: => String): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod)
  //@inline @elidable(FINER)
  //final def entering(sourceClass: => String, sourceMethod: => String, param1: Object): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod, param1)
  @inline @elidable(FINER)
  final def entering(sourceClass: => String, sourceMethod: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod, params)
  @inline @elidable(FINER)
  final def exiting(sourceClass: => String, sourceMethod: => String): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.exiting(sourceClass, sourceMethod)
  @inline @elidable(FINER)
  final def exiting(sourceClass: => String, sourceMethod: => String, result: Object): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.exiting(sourceClass, sourceMethod, result)
  @inline @elidable(FINER)
  final def throwing(sourceClass: => String, sourceMethod: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.throwing(sourceClass, sourceMethod, thrown)
  @inline @elidable(SEVERE)
  final def severe(msg: => String): Unit = if (julLogger.isLoggable(Level.SEVERE)) julLogger.severe(msg)
  @inline @elidable(WARNING)
  final def warning(msg: => String): Unit = if (julLogger.isLoggable(Level.WARNING)) julLogger.warning(msg)
  @inline @elidable(INFO)
  final def info(msg: => String): Unit = if (julLogger.isLoggable(Level.INFO)) julLogger.info(msg)
  @inline @elidable(CONFIG)
  final def config(msg: => String): Unit = if (julLogger.isLoggable(Level.CONFIG)) julLogger.config(msg)
  @inline @elidable(FINE)
  final def fine(msg: => String): Unit = if (julLogger.isLoggable(Level.FINE)) julLogger.fine(msg)
  @inline @elidable(FINER)
  final def finer(msg: => String): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.finer(msg)
  @inline @elidable(FINEST)
  final def finest(msg: => String): Unit = if (julLogger.isLoggable(Level.FINEST)) julLogger.finest(msg)

  def logAllToStderr() {
    julLogger.setLevel(Level.ALL)
    val ch = new java.util.logging.ConsoleHandler()
    ch.setLevel(Level.ALL)
    julLogger.addHandler(ch)
  }

  @elidable(elidable.ASSERTION) @inline
  final def check(assertion: Boolean, message: => Any) {
    if (!assertion)
      log(Level.SEVERE, "Check failed.", new java.lang.Exception("check failed: " + message))
  }

}
