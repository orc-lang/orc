//
// CompilerFlagValue.scala -- Compiler flag values that convert cleanly
// Project OrcScala
//
// Created by amp on Oct 3, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile

import orc.error.compiletime.SyntacticException
import orc.error.compiletime.SeverityError

class CompilerConfigurationException(val message: String)
  extends SyntacticException(message)
  with SeverityError

/** This represents a string argument but provides conversion functions that
  * throw compilation exceptions when a failures occur and provides better
  * support for defaults.
  *
  * @author amp
  */
final class CompilerFlagValue(name: String, val s: Option[String]) {
  def parseFlexibleBoolean(s: String) = {
    if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on") || s.equalsIgnoreCase("yes"))
      true
    else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off") || s.equalsIgnoreCase("no"))
      false
    else
      throw new IllegalArgumentException();
  }

  def asInt(d: => Int = { throw new CompilerConfigurationException(s"Flag $name has invalid value '$s'.") }) = {
    s match {
      case Some(s) =>
        try {
          s.toInt
        } catch {
          case _: NumberFormatException => {
            val v = d
            // TODO: This could print a warning.
            v
          }
        }
      case None =>
        d
    }
  }

  def asBool(d: => Boolean = false): Boolean = {
    s match {
      case Some(s) =>
        try {
          parseFlexibleBoolean(s)
        } catch {
          case _: IllegalArgumentException => {
            val v = d
            // TODO: This could print a warning.
            v
          }
        }
      case None =>
        d
    }
  }

  def asString(d: => String): String = {
    s match {
      case Some(s) =>
        try {
          s
        } catch {
          case _: IllegalArgumentException => {
            val v = d
            // TODO: This could print a warning.
            v
          }
        }
      case None =>
        d
    }
  }

  override def toString = s.getOrElse("Not set")
}
