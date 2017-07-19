//
// SwivelSmokeTest.scala -- Scala object SwivelSmokeTest
// Project OrcScala
//
// Created by amp on Jun 11, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import swivel.root
import swivel.branch
import swivel.leaf
import swivel.subtree
import swivel.EmptyFunction
import swivel.TransformFunction
import swivel.replacement

object SwivelSmokeTest {
  class Transform extends TransformFunction {
    val onExpression: PartialFunction[Expression.Z, Expression] = EmptyFunction
    val onArgument: PartialFunction[Argument.Z, Argument] = EmptyFunction
    
    def apply(e: Expression.Z) = transformWith[Expression.Z, Expression](e)(this, onExpression)
    def apply(e: Argument.Z) = transformWith[Argument.Z, Argument](e)(this, onArgument)
  }
  
  @root @transform[Transform]
  sealed abstract class AST {
    def test1() = 1
  }
  
  @branch @replacement[Def]
  sealed abstract class Def extends AST {
    self =>
    def test2() = 2
  }
  
  @leaf @transform
  final case class DefRoutine(name: BoundVar, formals: Seq[BoundVar], @subtree body: Expression) extends Def
  
  @branch @replacement[Expression]
  sealed abstract class Expression extends AST
  
  @branch
  sealed abstract class Argument extends Expression
  
  @leaf @transform
  final class BoundVar(val name: String) extends Argument {
    def copy(name: String = this.name) = new BoundVar(name)
  }
  
  @leaf @transform
  final case class Call(@subtree target: Argument, @subtree args: Option[Seq[Argument]]) extends Expression
  @leaf @transform
  final case class Branch(@subtree left: Expression, x: BoundVar, @subtree right: Expression) extends Expression
  
  def main(args: Array[String]): Unit = {
    val y = new BoundVar("y")
    val e = Branch(Call(new BoundVar("x"), Some(List())), y, y)
    val z: Branch.Z = e.toZipper()
    val Branch.Z(Call.Z(x, args), y2, yz) = z
    println((x: Argument.Z, args: Option[Seq[Argument.Z]], y2: BoundVar, yz: Expression.Z))
    (new Transform)(z)
    
    z.replace(e: Expression)
    
    e.test1()
    
    
    val d = DefRoutine(y, List(), e)
    val zd = d.toZipper()
    //zd.replace(e)
    zd.body.replace(e)
    
    d.test2()
  }
}
