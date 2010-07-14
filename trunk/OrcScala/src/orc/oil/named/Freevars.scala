//
// Freevars.scala -- Scala class/trait/object Freevars
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 13, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.oil.named

/**
 * 
 *
 * @author dkitchin
 */

trait hasFreeVars {
  self : NamedAST =>
  
  /* Note: As is evident from the type, UnboundVars are not included in this set */
  lazy val freevars: Set[BoundVar] = {
    val varset = new scala.collection.mutable.HashSet[BoundVar]()
    val collect = new NamedASTTransform {
      override def onArgument(context: List[BoundVar]) = {
        case x : BoundVar => (if (context contains x) { } else { varset += x }) ; x
      }
    }
    collect(this)
    Set.empty ++ varset
  }

}

trait hasFreeTypeVars {
  self : NamedAST =>
  
  /* Note: As is evident from the type, UnboundTypevars are not included in this set */
  lazy val freetypevars: Set[BoundTypevar] = {
    val varset = new scala.collection.mutable.HashSet[BoundTypevar]()
    val collect = new NamedASTTransform {
      override def onType(typecontext: List[BoundTypevar]) = {
        case u: BoundTypevar => if (typecontext contains u) {} else { varset += u } ; u
      }
    }
    collect(this)
    Set.empty ++ varset
  }

}