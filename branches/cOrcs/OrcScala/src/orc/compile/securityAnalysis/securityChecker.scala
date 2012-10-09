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

  val verbose = false
  type Context = Map[syntactic.BoundVar, SecurityLevel]

  def apply(expr: Expression): (Expression, SecurityLevel) = {
    slSynthExpr(expr)(Map.empty)
  }

  /** synthesis the security level for a given expression
    */
  def slSynthExpr(expr: Expression)(implicit context: Context): (Expression, SecurityLevel) = {
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
            (FoldedCall(target,args,typeArgs), resultSL)
          }
          case left || right => {
            val (newLeft, slLeft) = slSynthExpr(left)
            val (newRight, slRight) = slSynthExpr(right)
            (newLeft || newRight, SecurityLevel.meet(slLeft, slRight))
          }
          case left ow right => {// the syntax for this is (left ; right)
            val (newLeft, slLeft) = slSynthExpr(left)
            val (newRight, slRight) = slSynthExpr(right)
            (newLeft ow newRight, SecurityLevel.meet(slLeft, slRight))
          }
          case left > x > right => {
            val (newLeft, slLeft) = slSynthExpr(left)
            val (newRight, slRight) = slSynthExpr(right)(context + ((x, slLeft)))
            (newLeft > x > newRight, slRight)
          }
          case left < x < right => {
            val (newRight, slRight) = slSynthExpr(right)
            val (newLeft, slLeft) = slSynthExpr(left)(context + ((x, slRight)))
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
            if(verbose){
              System.out.println("hasSecurityLevel synth BODY: " + body + " LEVEL: " + level)
            }
            val expectedSL = SecurityLevel.findByName(level)
            if(expectedSL == null)
              throw new SecurityException("Attached SL: " + level + " does not exist in the lattice", new Exception())
            val newBody = slCheckExpr(body, expectedSL)
            
            
            
            (HasSecurityLevel(newBody, level), expectedSL)
          }
          //create security level
          case DeclareSecurityLevel(name, parents, children, body) => {
            //add the security level
            val declaredSecurityLevel = SecurityLevel.interpretParseSL(name, parents, children)
            val (newBody, bodySL) = slSynthExpr(body)(context)
            (DeclareSecurityLevel(name, parents, children, newBody), bodySL)
          }

        }
      (expr ->> newExpr, exprSL)
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
          val (e, foldedSL) = slFoldedCall(target, args, typeArgs)
          if (foldedSL != null) //we only check for when SL are used
          {
            //check that the foldedSL is equal to the expected SL (lattice) or can be written to
            //if it isn't then we throw an exception
            if (!(SecurityLevel.canWrite(lattice, foldedSL)))
              throw new SecurityException("SECURITY ERROR: Expression: " + e + "\nSecurityLevel: " +
                foldedSL + " is not " +
                " allowed to be written to security level " + lattice + ". INTEGRITY ERROR", new Exception())
          }
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
          val declaredSecurityLevel = SecurityLevel.interpretParseSL(name, parents, children)
          val (newBody, bodySL) = slSynthExpr(body)(context)
          DeclareSecurityLevel(name, parents, children, newBody)
        }
        case _ => {
          val (newExpr, exprSL) = slSynthExpr(expr)
          if(verbose){Console.println("EXPR: " + newExpr + " ; LEVEL: " + exprSL)}
          if (exprSL != null) //we only check for when SL are used
          {
            //check that the exprSL is equal to the expected SL (lattice)
            //if it isn't then we throw an exception
            if (!(SecurityLevel.canWrite(lattice, exprSL)))
              throw new SecurityException("SECURITY ERROR: Expression: " + newExpr + "\nSecurityLevel: " +
                exprSL + " is not " +
                " allowed to be written to security level " + lattice + ". INTEGRITY ERROR", new Exception())
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
    if(verbose){
      Console.println("TARGET: " + target + " SITE: " + site + " siteSL: " + siteSl)
    }
    var newSiteSl = siteSl
    val (newArgs, argSls) = (args map slSynthExpr).unzip //get the sl for each of the arguments

    if(verbose){
      for (argSL <- argSls) Console.println("Site argument SLs: " + argSL)
    }
    
    //for each argument, we must check that the site can write to them (integrity)
    //so, the site must have lower sL than the arguments (shouldn't be able to write high level info to lower level things)
    for (argLevel <- argSls) {
          newSiteSl = SecurityLevel.meet(newSiteSl, argLevel)
          if(verbose){
          Console.println("MEET for EXPR: " + site + " (" + argLevel + ") = " + newSiteSl)
        }
    }
      if (SecurityLevel.canWrite(newSiteSl, newSiteSl) == false) //checks is siteSL can write to argLevel
      {
        throw new SecurityException("SECURITY ERROR: Site (" + site + ") of level " + siteSl + " cannot" +
          " write to argument of level " + newSiteSl + ".", new Exception())
      } 
    
    if(verbose){Console.println("FINAL SL: " + newSiteSl)}
    (FoldedCall(site, newArgs, syntacticTypeArgs), newSiteSl) //return SL of a call is the security level published
  }
}