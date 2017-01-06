//
// TranslateVclock.scala -- Scala class TranslateVclock
// Project OrcScala
//
// Created by jthywiss on Jan 25, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.translate

import orc.ast.oil.named.{ BoundTypevar, BoundVar, Call, Constant, NamedASTTransform, Sequence, VtimeZone }
import orc.error.OrcExceptionExtension.extendOrcException
import orc.error.compiletime.{ CompilationException, ContinuableSeverity, IncorrectVclockCall, InvalidVclockUse }
import orc.lib.time.Vclock

/** Translate calls to the Vclock quasi-site into VtimeZone expressions.
  *
  * @author jthywiss
  */
class TranslateVclock(val reportProblem: CompilationException with ContinuableSeverity => Unit) extends NamedASTTransform {

  override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {

    case Sequence(Call(Constant(`Vclock`), List(arg), None), x, right) =>
      // This originally had the additional check to make sure x was not used by making sure it did not have a name.
      // However assigning names to all variables is useful, to the check was removed.
      // TODO: Add a way to distinguish synthetic from user variable names and add check to require synthetic name
      VtimeZone(arg, transform(right, context, typecontext))

    // Match malformed Vclock calls: wrong args, use of typeargs, binding a var in the sequence:
    case e @ Sequence(c @ Call(Constant(`Vclock`), args, typeargs), x, right) =>
      { reportProblem(IncorrectVclockCall() at c); e }

    // Match other uses of the Vclock site:
    case c @ Constant(`Vclock`) =>
      { reportProblem(InvalidVclockUse() at c); c }

  }

}
