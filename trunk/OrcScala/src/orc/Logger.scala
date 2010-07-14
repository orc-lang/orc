//
// Logger.scala -- Scala class Logger
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc

import java.util.logging.Level

/**
 * A Scala wrapper around java.util.logging.Logger
 *
 * @author jthywiss
 */
class Logger(name: String) {
  lazy val julLogger: java.util.logging.Logger = java.util.logging.Logger.getLogger(name)
  
  //TODO: LogRecord caller and method will be wrong -- need to write our own inferCaller and use
  def log(level: Level, msg: => String): Unit = if (julLogger.isLoggable(level)) julLogger.log(level, msg)
  def log(level: Level, msg: => String, param1: Object): Unit = if (julLogger.isLoggable(level)) julLogger.log(level, msg, param1)
  def log(level: Level, msg: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(level)) julLogger.log(level, msg, params)
  def log(level: Level, msg: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(level)) julLogger.log(level, msg, thrown)
  def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg)
  def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, param1: Object): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, param1)
  def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, params)
  def logp(level: Level, sourceClass: => String, sourceMethod: => String, msg: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(level)) julLogger.logp(level, sourceClass, sourceMethod, msg, thrown)
  def logrb(level: Level, sourceClass: => String, sourceMethod: => String, bundleName: => String, msg: => String): Unit = if (julLogger.isLoggable(level)) julLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg)
  def logrb(level: Level, sourceClass: => String, sourceMethod: => String, bundleName: => String, msg: => String, param1: Object): Unit = if (julLogger.isLoggable(level)) julLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1)
  def logrb(level: Level, sourceClass: => String, sourceMethod: => String, bundleName: => String, msg: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(level)) julLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, params)
  def logrb(level: Level, sourceClass: => String, sourceMethod: => String, bundleName: => String, msg: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(level)) julLogger.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown)
  def entering(sourceClass: => String, sourceMethod: => String): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod)
  def entering(sourceClass: => String, sourceMethod: => String, param1: Object): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod, param1)
  def entering(sourceClass: => String, sourceMethod: => String, params: => Seq[Object]): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.entering(sourceClass, sourceMethod, params)
  def exiting(sourceClass: => String, sourceMethod: => String): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.exiting(sourceClass, sourceMethod)
  def exiting(sourceClass: => String, sourceMethod: => String, result: Object): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.exiting(sourceClass, sourceMethod, result)
  def throwing(sourceClass: => String, sourceMethod: => String, thrown: Throwable): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.throwing(sourceClass, sourceMethod, thrown)
  def severe(msg: => String): Unit = if (julLogger.isLoggable(Level.SEVERE)) julLogger.severe(msg)
  def warning(msg: => String): Unit = if (julLogger.isLoggable(Level.WARNING)) julLogger.warning(msg)
  def info(msg: => String): Unit = if (julLogger.isLoggable(Level.INFO)) julLogger.info(msg)
  def config(msg: => String): Unit = if (julLogger.isLoggable(Level.CONFIG)) julLogger.config(msg)
  def fine(msg: => String): Unit = if (julLogger.isLoggable(Level.FINE)) julLogger.fine(msg)
  def finer(msg: => String): Unit = if (julLogger.isLoggable(Level.FINER)) julLogger.finer(msg)
  def finest(msg: => String): Unit = if (julLogger.isLoggable(Level.FINEST)) julLogger.finest(msg)
}
