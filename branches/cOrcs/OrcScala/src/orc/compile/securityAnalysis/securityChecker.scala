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
import orc.ast.oil.named.{ Expression, Stop, Hole, Call, ||, ow, <, >, DeclareDefs, HasType, DeclareType, Constant, UnboundVar, Def, FoldedCall, FoldedLambda, DeclareSecurityLevel, HasSecurityLevel }
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

/** @author laurenyew
  * "TypeChecker" AKA SecurityAnalysis for SecurityLevels
  * Step called by compiler
  * This is for INTEGRITY static type checking
  * (Confidentiality static type checking is the opposite of integrity)
  * Possible future proj: add option to switch between checking for integrity and for confidentiality
  */
object securityChecker {

  type Context = Map[syntactic.BoundVar, SecurityLevel]

  def apply(expr: Expression): (Expression, SecurityLevel) = {
    slSynthExpr(expr)(Map.empty)
  }

  /** synthesis the security level for a given expression
    */
  def slSynthExpr(expr: Expression)(implicit context: Context): (Expression, SecurityLevel) = {
  //  Console.println("EXPR: " + expr + "\nCONTEXT " + context)
    try {
      val (newExpr, exprSL) =
        expr match {
          case Stop() => (expr, null)
          case Hole(_, _) => (expr, null)
          case Constant(value) => (expr, null)
          case x: syntactic.BoundVar => {
            var level: SecurityLevel = null
            try {
              level = context(x) //we now have the expr key and the contextSL value
            } catch {
              case e: java.util.NoSuchElementException => { level = null } //if no value mapped then send null
            }
            (x, level) //if value worked send it thru

          }
          case UnboundVar(name) => throw new UnboundVariableException(name)
          //This is when a site calls on arguments,
          //wee need to see if the target should be able to call
          case FoldedCall(target, args, typeArgs) => {
            val (result, resultSL) = slFoldedCall(target, args, typeArgs)
            //Console.println("FINAL result: " + resultSL)
            (FoldedCall(target,args,typeArgs), resultSL)
          }
          case left || right => {
            val (newLeft, slLeft) = slSynthExpr(left)
            val (newRight, slRight) = slSynthExpr(right)
  //          Console.println("MEET for EXPR: " + newLeft + " || " + newRight)
            (newLeft || newRight, SecurityLevel.meet(slLeft, slRight))
          }
          case left ow right => {// the syntax for this is (left ; right)
            val (newLeft, slLeft) = slSynthExpr(left)
            val (newRight, slRight) = slSynthExpr(right)
     //       Console.println("MEET for EXPR: " + newLeft + " ow " + newRight)
            (newLeft ow newRight, SecurityLevel.meet(slLeft, slRight))
          }
          case left > x > right => {
            val (newLeft, slLeft) = slSynthExpr(left)
            val (newRight, slRight) = slSynthExpr(right)(context + ((x, slLeft)))
     //       Console.println("MEET for EXPR: " + newLeft + " >x> " + newRight)
            (newLeft > x > newRight, slRight)
          }
          case left < x < right => {
            val (newRight, slRight) = slSynthExpr(right)
            val (newLeft, slLeft) = slSynthExpr(left)(context + ((x, slRight)))
   //         Console.println("MEET for EXPR: " + newLeft + " <x< " + newRight)
            (newLeft < x < newRight, slLeft)
          }
          case DeclareDefs(defs, body) => {
            val (newBody, bodySl) = slSynthExpr(body)(context)
            (DeclareDefs(defs, newBody), bodySl)
          }
          //checks if the body has the correct type
          case HasType(body, syntactic.AssertedType(t)) => {
            val (newBody, bodySl) = slSynthExpr(body)
            (expr, bodySl)
          }
          case HasType(body, t) => {
            val (newBody, bodySl) = slSynthExpr(body)
            (HasType(body, t), bodySl)
          }
          case DeclareType(u, t, body) => {
            val (newBody, bodySl) = slSynthExpr(body)
            (DeclareType(u, t, body), bodySl)
          }
          /** checks if the body has the correct security Level
            * if an exception is not thrown (the expectedSL checks out)
            * then adds the SL successfully
            */
          case HasSecurityLevel(body, level) => {
            // System.out.println("hasSecurityLevel synth")
            val expectedSL = SecurityLevel.findByName(level)
            val newBody = slCheckExpr(body, expectedSL)
            (HasSecurityLevel(newBody, level), expectedSL)
          }
          //create security level
          case DeclareSecurityLevel(name, parents, children, body) => {
            //  System.out.println("DeclareSecurityLevel synth")
            //add the security level
            val declaredSecurityLevel = SecurityLevel.interpretParseSL(name, parents, children)
            val (newBody, bodySL) = slSynthExpr(body)(context)
            (DeclareSecurityLevel(name, parents, children, newBody), bodySL)
          }

        }
      (expr ->> newExpr, exprSL)
     // Console.println("EXPR_AFTER: " + newExpr +"\nSL: " + exprSL + "\nCONTEXT_AFTER " + context + "\n")
      (newExpr, exprSL)
    } catch {
      case e: TypeException => {
        throw (e.setPosition(expr.pos))
      }
    }

  }

  //expr encompasses hasSecurityLevel
  //don't need patterns
  //just do cases over expressions
  def slCheckExpr(expr: Expression, lattice: SecurityLevel)(implicit context: Context): Expression = {
    try {
      expr -> {
        /* FoldedCall must be checked before prune, since it
         * may contain some number of enclosing prunings.
         */
        case FoldedCall(target, args, typeArgs) => {
          val (e, _) = slFoldedCall(target, args, typeArgs)
          e
        }
        case left || right => {
          val newLeft = slCheckExpr(left, lattice)
          val newRight = slCheckExpr(right, lattice)
          newLeft || newRight
        }
        case left ow right => {
          val newLeft = slCheckExpr(left, lattice)
          val newRight = slCheckExpr(right, lattice)
          newLeft ow newRight
        }
        case left > x > right => {
          val (newLeft, slLeft) = slSynthExpr(left)
          //call self again (recursively adding in new context on right)
          val newRight = slCheckExpr(right, lattice)(context + ((x, slLeft)))
          newLeft > x > newRight
        }
        case left < x < right => {
          val (newRight, slRight) = slSynthExpr(right)
          val newLeft = slCheckExpr(left, lattice)(context + ((x, slRight)))
          newLeft < x < newRight
        }
        case DeclareSecurityLevel(name, parents, children, body) => {
          //add the security level
          //  System.out.println("declareSecurityLevel Check")
          val declaredSecurityLevel = SecurityLevel.interpretParseSL(name, parents, children)
          val (newBody, bodySL) = slSynthExpr(body)(context)
          DeclareSecurityLevel(name, parents, children, newBody)
        }
        case _ => {
          val (newExpr, exprSL) = slSynthExpr(expr)
          //  System.out.println("check Expr _ SL: " + exprSL)
          if (exprSL != null) //we only check for when SL are used
          {
            //check that the exprSL is equal to the expected SL (lattice)
            //if it isn't then we throw an exception
            if (!(exprSL eq lattice))
              throw new Exception("SECURITY ERROR: Expression: " + newExpr + "\nSecurityLevel: " +
                exprSL + "does not " +
                "equal the expected security level: " + lattice)
          }
          newExpr
        }
      }
    } catch {
      case e: TypeException => {
        throw (e.setPosition(expr.pos))
      }
    }
  }

  /** Check for calls to sites
    */
  def slFoldedCall(target: Expression, args: List[Expression], syntacticTypeArgs: Option[List[syntactic.Type]])(implicit context: Context): (Expression, SecurityLevel) = {
    val (site, siteSl) = slSynthExpr(target) //site targetSL
    var newSiteSl = siteSl
    val (newArgs, argSls) = (args map slSynthExpr).unzip //get the sl for each of the arguments

    //for each argument, we must check that the site can write to them (integrity)
    //so, the site must have lower sL than the arguments (shouldn't be able to write high level info to lower level things)
    for (argLevel <- argSls) {

      if (SecurityLevel.canWrite(siteSl, argLevel) == false) //checks is siteSL can write to argLevel
      {
        throw new Exception("SECURITY ERROR: Site (" + site + ") of level " + siteSl + " cannot" +
          " write to argument of level " + argLevel + ".")
      } else //if you can write to the siteSl, you may have moved down the results' integrity
      {
        Console.println("MEET for EXPR: " + site + " (" + argLevel + ")")
        newSiteSl = SecurityLevel.meet(newSiteSl, argLevel)
      }
    }
    //Console.println("FINAL SL: " + newSiteSl)
    (FoldedCall(site, newArgs, syntacticTypeArgs), newSiteSl) //return SL of a call is the security level published
  }
}