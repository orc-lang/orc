//
// ExceptionCompileLogger.scala -- Scala class/trait/object ExceptionCompileLogger
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 8, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.error.compiletime

import orc.error.compiletime.CompileLogger.Severity
import orc.AST
import scala.util.parsing.input.Position

/**
 * A CompileMessageRecorder that throws an exception on a message of
 * severity WARNING or higher.
 *
 * @author jthywiss
 */
class ExceptionCompileLogger extends CompileLogger {
    private var maxSeverity = Severity.UNKNOWN;

    /* (non-Javadoc)
     * @see orc.error.compiletime.CompileLogger#beginProcessing(java.lang.String)
     */
    def beginProcessing(filename: String) {
        maxSeverity = Severity.UNKNOWN;
    }

    /* (non-Javadoc)
     * @see orc.error.compiletime.CompileLogger#endProcessing(java.lang.String)
     */
    def endProcessing(filename: String) {
        // Nothing needed
    }

    /* (non-Javadoc)
     * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, AST, Throwable)
     */
    def recordMessage(severity: Severity, code: Int, message: String, location: Position, astNode: AST, exception: Throwable) {

        maxSeverity = if (severity.ordinal() > maxSeverity.ordinal()) severity else maxSeverity

        if (severity.ordinal() >= Severity.WARNING.ordinal()) {
          if (exception != null) {
            throw exception;
          } else {
            if (location != null) {
              throw new CompilationException(location.toString() + ": " + message + "\n" + location.longString)
            } else {
              throw new CompilationException("<undefined position>: " + message)
            }
          }
        } // else disregard
    }

    /* (non-Javadoc)
     * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, Throwable)
     */
    def recordMessage(severity: Severity, code: Int, message: String, location: Position, exception: Throwable) {
        recordMessage(severity, code, message, location, null, exception)
    }

    /* (non-Javadoc)
     * @see orc.error.compiletime.CompileLogger#recordMessage(Severity, int, String, Position, AST)
     */
    def recordMessage(severity: Severity, code: Int, message: String, location: Position, astNode: AST) {
        recordMessage(severity, code, message, location, astNode, null)
    }

    /* (non-Javadoc)
     * @see orc.error.compiletime.CompileLogger#recordMessage(orc.error.compiletime.CompileLogger.Severity, int, java.lang.String)
     */
    def recordMessage(severity: Severity, code: Int, message: String) {
        recordMessage(severity, code, message, null, null, null)
    }

    /* (non-Javadoc)
     * @see orc.error.compiletime.CompileLogger#getMaxSeverity()
     */
    def getMaxSeverity(): Severity = maxSeverity

}