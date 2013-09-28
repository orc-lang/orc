//
// TranslateVclock.scala -- Scala class TranslateVclock
// Project OrcScala
//
// $Id: TranslateVclock.scala 3313 2013-09-26 02:38:29Z arthur.peters@gmail.com $
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

import orc.ast.oil.named._
import orc.error.compiletime.{ ContinuableSeverity, CompilationException }
import orc.error.OrcExceptionExtension.extendOrcException
import orc.lib.time.Vclock
import orc.lib.builtin.MakeResilient

/** Translate calls to the Vclock quasi-site into VtimeZone expressions.
  *
  * @author jthywiss
  */
class TranslateMakeResilient(val reportProblem: CompilationException with ContinuableSeverity => Unit) extends NamedASTTransform {

  override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
    case Call(Constant(mr: MakeResilient), List(f), None) => 
      val arity = mr.n
      val f1 = new BoundVar()
      val formals = (1 to arity) map (_ => new BoundVar()) toList
      val lam = Def(f1, formals, Resilient(Call(f, formals, None)), List(), None, None)
      DeclareDefs(List(lam), f1)
  }

}
