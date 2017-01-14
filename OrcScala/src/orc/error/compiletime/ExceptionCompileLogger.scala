//
// ExceptionCompileLogger.scala -- Scala class ExceptionCompileLogger
// Project OrcScala
//
// Created by jthywiss on Jun 8, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.compiletime

import orc.ast.AST
import orc.compile.parse.{ OrcInputContext, OrcSourceRange }
import orc.error.compiletime.CompileLogger.Severity

/** A CompileMessageRecorder that throws an exception on a message of
  * severity WARNING or higher.
  *
  * @author jthywiss
  */
class ExceptionCompileLogger() extends CompileLogger {
  private var maxSeverity = Severity.UNKNOWN;

  def beginProcessing(inputContext: OrcInputContext) {
    maxSeverity = Severity.UNKNOWN;
  }

  def endProcessing(inputContext: OrcInputContext) {
    // Nothing needed
  }

  def beginDependency(inputContext: OrcInputContext) {
    // Nothing needed
  }

  def endDependency(inputContext: OrcInputContext) {
    // Nothing needed
  }

  /* (non-Javadoc)
     * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, AST, Throwable)
     */
  def recordMessage(severity: Severity, code: Int, message: String, location: Option[OrcSourceRange], astNode: AST, exception: Throwable) {

    maxSeverity = if (severity.ordinal() > maxSeverity.ordinal()) severity else maxSeverity

    ExceptionCompileLogger.throwExceptionIfNeeded(Severity.WARNING, severity, message, location, exception)
  }

  def recordMessage(severity: Severity, code: Int, message: String, location: Option[OrcSourceRange], exception: Throwable) {
    recordMessage(severity, code, message, location, null, exception)
  }

  def recordMessage(severity: Severity, code: Int, message: String, location: Option[OrcSourceRange], astNode: AST) {
    recordMessage(severity, code, message, location, astNode, null)
  }

  def recordMessage(severity: Severity, code: Int, message: String) {
    recordMessage(severity, code, message, null, null, null)
  }

  def getMaxSeverity(): Severity = maxSeverity

}

object ExceptionCompileLogger {  
  class GenericCompilationException(message: String) extends CompilationException(message)

  def throwExceptionIfNeeded(minSeverity: Severity, severity: Severity, message: String, location: Position, exception: Throwable) {
    if (severity.ordinal() >= minSeverity.ordinal()) {
      if (exception != null) {
        throw exception;
      } else {
        // We don't have an exception to throw -- use our "fake" one
        val e = new GenericCompilationException(message)
        if (location != null) {
          e.setPosition(location)
        }
        throw e
      }
    } // else disregard
  }
}
