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
 * This is for INTEGRITY static type checking
 * (Confidentiality static type checking is the opposite of integrity)
 * Possible future proj: add option to switch between checking for integrity and for confidentiality
 */
object securityChecker{
  
  
  
  //expr encompasses hasSecurityLevel
  //don't need patterns
  //just do cases over expressions
  def securityCheck(expr: Expression, lattice: SecurityLevel): SecurityLevel =
  {
    lattice.initializeGraph()//initialize the graph of Security Levels
    try{
      expr match{
       case  Stop() =>  SecurityLevel.bottom//do nothing 
       /*
        case  SecurityLevelDeclaration(name, parents, children, body) =>
         {
           lattice.interpretParseSL(name, parents, children)//will return the security level declared
         }
         case HasSecurityLevel(body, level) =>
         {
           findByName(level) //finds the security level in lattice and returns, propagativing then with the variable?
           //may need to make a list of the variables? hope not
         } 
       */
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
           
           lattice.findClosestChild(leftSL,rightSL)
         }
       
       case Constant(value) => lattice //no effect on security level
       case x: syntactic.BoundVar => {
        //need to get the SL on the bound variable 
         lattice //hopefully it is the propagaed security level, otherwise this will make it bottom level
       }
       case left ow right => {//... not sure what ow is. Need to look up
         val leftSL = securityCheck(left,lattice)
          val rightSL = securityCheck(right,lattice)
         
         lattice.findClosestChild(leftSL,rightSL)
       
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