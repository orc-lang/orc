//
// PositionChecker.scala -- Scala object PositionChecker
// Project OrcScala
//
// Created by amp on Jul, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.orctimizer.named

import orc.compile.CompilerOptions
import orc.error.compiletime.InternalCompilerError
import orc.error.compiletime.InternalCompilerWarning

object PositionChecker {
  def apply(e: NamedAST.Z, co: CompilerOptions): Unit = {
    checker(co)(e)
  }
  
  private def checkError(b: Boolean, msg: => String, e: NamedAST.Z)(implicit co: CompilerOptions): Unit = if (!b) {
    val exc = new InternalCompilerError(msg)
    e.value.sourceTextRange foreach { exc.setPosition(_) }
    co.reportProblem(exc)
  }
  
  private def checkWarning(b: Boolean, msg: => String, e: NamedAST.Z)(implicit co: CompilerOptions): Unit = if (!b) {
    val exc = new InternalCompilerWarning(msg)
    e.value.sourceTextRange foreach { exc.setPosition(_) }
    co.reportProblem(exc)
  }
  
  val prefixLength = 50
  
  private def checker(implicit co: CompilerOptions) = new Transform {
    override val onExpression: PartialFunction[Expression.Z, Expression] = {
      case e =>
        checkWarning(e.value.sourceTextRange.isDefined, s"The expression beginning with ${e.value.toString().take(prefixLength)} does not have source information associated with it.", e)
        e.value
    }
  
    override val onMethod: PartialFunction[Method.Z, Method] = {
      case e =>
        checkWarning(e.value.sourceTextRange.isDefined, s"The method beginning with ${e.value.toString().take(prefixLength)} does not have source information associated with it.", e)
        e.value
    }
  
    override val onArgument: PartialFunction[Argument.Z, Argument] = {
      case v =>
        checkWarning(v.value.sourceTextRange.isDefined, s"The argument ${v.value.toString().take(prefixLength)} does not have source information associated with it.", v)
        v.value
    }
  }
}
