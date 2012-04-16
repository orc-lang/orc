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
//have to add in named expressions that I am importing
import orc.ast.oil.named.{ Expression, Stop, Hole, Call, ||, ow, <, >, DeclareDefs, HasType, DeclareType, Constant, UnboundVar, Def, FoldedCall, FoldedLambda , DeclareSecurityLevel, HasSecurityLevel}
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
 * @author laurenyew
 * "TypeChecker" AKA SecurityAnalysis for SecurityLevels
 * Step called by compiler
 * This is for INTEGRITY static type checking
 * (Confidentiality static type checking is the opposite of integrity)
 * Possible future proj: add option to switch between checking for integrity and for confidentiality
 */
object securityChecker{
  
  /**
   * Map function between Security Levels and their variables.
   */
  
  /**
   * find the securityLevel associated with the expression in the map
   */
  def context(expr: Expression) : SecurityLevel = 
  {
    SecurityLevel.bottom
  }
  
  //expr encompasses hasSecurityLevel
  //don't need patterns
  //just do cases over expressions
  def securityCheck(expr: Expression, lattice: SecurityLevel): SecurityLevel =
  {
    
    try{
      expr match{
       case  Stop() =>  SecurityLevel.bottom//do nothing 
       
       case  DeclareSecurityLevel(name, parents, children, body) =>
       {
           SecurityLevel.interpretParseSL(name, parents, children)//will return the security level declared
       }
        
       case HasSecurityLevel(body, level) =>
       {
           SecurityLevel.findByName(level) //finds the security level in lattice and adds variable to the map
       } 
       
       /**
        * OR: outputs the union of the two inputs, so the security should be the 
        * closest common child of both security levels
        * Why: when the user looks at 2 items, one of high integrity vs one of low integrity
        * the information value of the total goes down (since we don't want to move up bad info)
        */
       case left || right => 
         {
            val leftSL = securityCheck(left,lattice)
            val rightSL = securityCheck(right,lattice)
           
           SecurityLevel.meet(leftSL,rightSL)
         }
       
       case Constant(value) => lattice //no effect on security level
       case x: syntactic.BoundVar => {
        //need to get the SL on the bound variable 
         lattice //hopefully it is the propagaed security level, otherwise this will make it bottom level
       }
       case left ow right => {//Otherwise
         val leftSL = securityCheck(left,lattice)
          val rightSL = securityCheck(right,lattice)
         SecurityLevel.meet(leftSL,rightSL)//you need to have the security to write left and right (in case of otherwise)
       }
       case left > x > right => {//left goes to x then goes to right
             val leftSL = securityCheck(left,lattice)
             val rightSL = securityCheck(right,lattice)
             SecurityLevel.meet(leftSL,rightSL)//you need to have the security to write left and right (in case of otherwise)
       }
       case left < x < right => {
           val leftSL = securityCheck(left,lattice)
             val rightSL = securityCheck(right,lattice)
             SecurityLevel.meet(leftSL,rightSL)//you need to have the security to write left and right (in case of otherwise)
       }
       
       
       case _ => lattice//if the target isn't found, evaluate as ok, send thru the current security level
       
      }
    }
    catch{//throw the exception if there is one
      case e: Exception => {
        throw e
      }
    }
  }
}