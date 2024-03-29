//
// VariableChecker.scala -- Scala object VariableChecker
// Project OrcScala
//
// Created by amp on Jul, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.orctimizer.named

import scala.collection.mutable

import orc.compile.CompilerOptions
import orc.error.compiletime.InternalCompilerError

object VariableChecker {
  def apply(e: NamedAST.Z, co: CompilerOptions): Unit = {
    checker(co)(e)
  }

  private def checkError(b: Boolean, msg: => String, e: NamedAST.Z)(implicit co: CompilerOptions): Unit = if (!b) {
    val exc = new InternalCompilerError(msg)
    e.value.sourceTextRange foreach { exc.setPosition(_) }
    co.reportProblem(exc)
  }

  //private def checkWarning(b: Boolean, msg: => String, e: NamedAST.Z)(implicit co: CompilerOptions): Unit = if (!b) {
  //  val exc = new InternalCompilerWarning(msg)
  //  e.value.sourceTextRange foreach { exc.setPosition(_) }
  //  co.reportProblem(exc)
  //}

  private def checker(implicit co: CompilerOptions) = new Transform {
    val encounteredVariables = mutable.HashSet[BoundVar]()

    override val onExpression: PartialFunction[Expression.Z, Expression] = {
      case e if e.value.boundVars.nonEmpty =>
        e.value.boundVars foreach { v =>
          checkError(!encounteredVariables.contains(v), s"Variable $v has already been declared in the program.", e)
          encounteredVariables += v
        }
        e.value
      case a: Argument.Z if onArgument.isDefinedAt(a) =>
        onArgument(a)
    }

    override val onMethod: PartialFunction[Method.Z, Method] = {
      case e if e.value.boundVars.nonEmpty =>
        e.value.boundVars foreach { v =>
          checkError(!encounteredVariables.contains(v), s"Variable $v has already been declared in the program.", e)
          encounteredVariables += v
        }
        e.value
    }

    override val onArgument: PartialFunction[Argument.Z, Argument] = {
      case v: BoundVar.Z =>
        checkError(v.contextBoundVars.contains(v.value), s"Variable is not declared in scope: $v", v)
        v.value
    }
  }
}
