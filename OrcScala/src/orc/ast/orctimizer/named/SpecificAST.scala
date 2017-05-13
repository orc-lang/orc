//
// SpecificAST.scala -- Scala class SpecificAST
// Project OrcScala
//
// Created by amp on Mar 17, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.orctimizer.named

import orc.ast.PrecomputeHashcode

// FIXME: This cannot distinguish identical subexpressions. For instance, in 1 | 1 the two "1"s are not different.
//        This is a pretty major problem since 1|1 publishes twice while 1 publishes once.
case class SpecificAST[+T <: NamedAST](ast: T, path: List[NamedAST]) extends PrecomputeHashcode {
  (ast :: path).tails foreach {
    case b :: a :: _ =>
      assert(a.subtrees.toSet contains b, s"Path ${path.map(SpecificAST.shortString).mkString("[", ", ", "]")} does not contain a parent of $ast.\n$b === is not a subtree of ===\n$a\n${a.subtrees}")
    case Seq(_) => true
    case Seq() => true
  }

  def subtreePath = ast :: path

  override def toString() = {
    s"$productPrefix($ast, ${path.map(SpecificAST.shortString).mkString("[", ", ", "]")})"
  }
}

object SpecificAST {
  import scala.language.implicitConversions
  private def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"

  implicit def SpecificAST2AST[T <: NamedAST](l: SpecificAST[T]): T = l.ast
  implicit class OptionSpecificASTAdds[T <: NamedAST](l: Option[SpecificAST[T]]) {
    def subtreePath = l.map(_.subtreePath).getOrElse(Nil)
  }
}
