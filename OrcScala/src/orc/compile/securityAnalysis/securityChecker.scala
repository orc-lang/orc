//
// TypeChecker.scala -- Scala object TypeChecker
// Project OrcScala
//
// $Id: Typechecker.scala 2933 2011-12-15 16:26:02Z jthywissen $
//
// Created by jthywiss on May 24, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.securityAnalysis

import orc.ast.oil.{ named => syntactic }
import orc.ast.oil.named.{ Expression, Stop, Hole, Call, ||, ow, <, >, DeclareDefs, HasType, DeclareType, Constant, UnboundVar, Def, FoldedCall, FoldedLambda }
import orc.types._
import orc.error.compiletime.typing._
import orc.error.compiletime.{ UnboundVariableException, UnboundTypeVariableException }
import orc.util.OptionMapExtension._
import orc.values.{ Signal, Field }
import scala.math.BigInt
import scala.math.BigDecimal
import orc.values.sites.TypedSite
import orc.compile.typecheck.ConstraintSet._
import orc.compile.typecheck.Typeloader._



/**
 * "TypeChecker" AKA SecurityAnalysis for SecurityLevels
 * Step called by compiler
 */
object securityChecker{
  
  //expr encompasses hasSecurityLevel
  //don't need patterns
  //just do cases over expressions
  def securityCheck(expr: Expression, lattice: SecurityLevel): SecurityLevel =
  {
    expr match{
     case  Stop() =>  lattice.findByName("BOTTOM")//do nothing 
    //  Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
      case left || right => {
        val leftSL = securityCheck(left,lattice)
        val rightSL = securityCheck(right,lattice)
        //find closest parent or top if no parent (JOIN)?
        lattice.findByName("BOTTOM")
      }
      /*
      case class Sequence(left: Expression, x: BoundVar, right: Expression) extends Expression
        with hasOptionalVariableName { transferOptionalVariableName(x, this) }
      case class Prune(left: Expression, x: BoundVar, right: Expression) extends Expression
        with hasOptionalVariableName { transferOptionalVariableName(x, this) }
      case class Otherwise(left: Expression, right: Expression) extends Expression
      case class DeclareDefs(defs: List[Def], body: Expression) extends Expression
      case class DeclareType(name: BoundTypevar, t: Type, body: Expression) extends Expression
        with hasOptionalVariableName { transferOptionalVariableName(name, this) }
      //DeclSL
      case DeclareSecurityLevel(name: String, parents: List[String], children: List[String]) => lattice.interpretParseST(name,parents,children)
      case class HasType(body: Expression, expectedType: Type) extends Expression
      //more generalized pattern -> Expression
      //so we write a HasSecurityLevel for expression to get pattern
      //Ex: for @A
      case HasSecurityLevel(body: Expression, level: String) => lattice.findByName(level)
      case class Hole(context: Map[String, Argument], typecontext: Map[String, Type]) extends Expression {
        def apply(e: Expression): Expression = e.subst(context, typecontext) 
        */
    }
  }
}