//
// CompilationExceptions.scala -- Scala child classes of CompilationException
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Aug 11, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.compiletime

import scala.util.parsing.input.Position

import orc.error.compiletime.CompileLogger.Severity

trait CompilationExceptionSeverity { }

/** Severity of this exception is internal, for tool debugging -- users don't care. */
trait SeverityDebug { val severity = Severity.DEBUG }
/** Severity of this exception is completely routine.  For example, counts of output size. */
trait SeverityInfo { val severity = Severity.INFO }
/** Severity of this exception is not routine, but not a problem. */
trait SeverityNotice { val severity = Severity.NOTICE }
/** Severity of this exception is a potential problem, but not bad enough to cause output to be disregarded -- it may still be usable. */
trait SeverityWarning { val severity = Severity.WARNING }
/** Severity of this exception is a problem that is severe enough that output was discarded or should be discarded -- it is not usable. */
trait SeverityError{ val severity = Severity.ERROR }
/** Severity of this exception is a problem that has caused input processing to be stopped. */
trait SeverityFatal { val severity = Severity.FATAL }
/** Severity of this exception is an internal failure of the tool (not the user's fault). */
trait SeverityInternal{ val severity = Severity.INTERNAL }

/**
 * Problem parsing the text of an Orc program. Mostly this
 * is a wrapper around the exceptions thrown by whatever
 * parsing library we use.
 */
class ParsingException(message: String, errorPos: Position) extends
  CompilationException(message) with SeverityError {
  setPosition(errorPos)
}

/**
 * 
 */
class PatternException(message: String) extends
  CompilationException(message) with SeverityError

/**
 * Indicate a problem with site resolution. Ideally
 * this would be a loadtime error, but currently site
 * resolution is done at runtime.
 */
class SiteResolutionException(val siteName: String, cause: Throwable) extends
  CompilationException("Problem loading site "+siteName, cause) with SeverityFatal

/**
 * 
 */
class UnboundVariableException(val varName: String) extends
  CompilationException("Variable " + varName + " is unbound") with SeverityError
