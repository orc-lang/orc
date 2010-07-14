//
// Substitution.scala -- Scala class/trait/object Substitution
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

trait Substitution[X <: NamedAST] {
  self : NamedAST =>
    
  def subst(a: Argument, x: Argument): X = Substitution(a,x)(this).asInstanceOf[X]
  def subst(a: Argument, s: String): X = Substitution(a, UnboundVar(s))(this).asInstanceOf[X]
  
  def substAll(sublist: List[(Argument, String)]): X = {
    val subs = new scala.collection.mutable.HashMap[Argument, Argument]()
    for ((a,s) <- sublist) {
      val x = UnboundVar(s)
      if ( subs.contains(x) )
        { this !! ("Conflicting substitutions on " + x + ": " + a + " vs " + subs(x)) }
      else 
        { subs += ( (x,a) ) }
    }
    Substitution.allArgs(subs)(this).asInstanceOf[X]
  }
  
  def subst(t: Type, u: Typevar): X = Substitution(t,u)(this).asInstanceOf[X]  
  def subst(t: Typevar, s: String): X = Substitution(t, UnboundTypevar(s))(this).asInstanceOf[X]
  def substAllTypes(sublist: List[(Type, String)]): X = {
    val subs = new scala.collection.mutable.HashMap[Typevar, Type]()
    for ((t,s) <- sublist) {
      val u = UnboundTypevar(s)
      if ( subs.isDefinedAt(u) )
        { this !! ("Conflicting substitutions on " + u + ": " + t + " vs " + subs(u)) }
      else 
        { subs += ( (u,t) ) }
    }
    Substitution.allTypes(subs)(this).asInstanceOf[X]
  }
  
  
  
}

object Substitution {
  
  def apply(a: Argument, x: Argument) =
    new NamedASTTransform {
      override def onArgument(context: List[BoundVar]) = { 
        case `x` => a
        case y : Argument => y
      }
    }
  
  def apply(t: Type, u: Typevar) =
    new NamedASTTransform {
      override def onType(typecontext: List[BoundTypevar]) = { 
        case `u` => t
        case w : Typevar => w
      }
    }
  
  def allArgs(subs: scala.collection.Map[Argument, Argument]) =
    new NamedASTTransform {
      override def onArgument(context: List[BoundVar]) = {
        case x : Var => {
          if ( subs.isDefinedAt(x) ) { subs(x) } else { x } 		
        }
      }
    }
  
  def allTypes(subs: scala.collection.Map[Typevar, Type]) =
    new NamedASTTransform {
      override def onType(typecontext: List[BoundTypevar]) = {
        case u : Typevar => {
          if ( subs.isDefinedAt(u) ) { subs(u) } else { u }
        }
      }
    }  
  
}
