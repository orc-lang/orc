//
// Transformer.scala -- Transformer object for Oil.
// Project OrcScala
//
// $Id: Oil.scala 1652 2010-05-27 18:33:35Z sidney.rosario $
//
// Created by sidney.rosario on May 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
  
package orc.oil

object Transformer {

  import orc.oil._
  
  /* Tags the tail calls in the body of definitions
   * in the input expression. */
  def tagTailCalls(e: Expression) {
	
	  object TailCallMarker extends OILWalker {
	    private def markTails(e: Expression) {
	      e match {
	        case c@ Call(target, args, typeArgs) => c.isTailCall = true
	        case f || g => markTails(f); markTails(g) 
	        case f >> g => markTails(g)
	        case f << g => markTails(f)
	        case f ow g => markTails(f); markTails(g) // ?? Verify this.
	        case DeclareDefs(defs, body) => markTails(body) 
	        case HasType(body,typ) => markTails(body) 
	      }
		}
	     
		override def enter(d : Def) {
			markTails(d.body)
		}
	 }
	 
	 TailCallMarker walk e
  }
  
  /** A basic walker class for OIL expressions */
  class OILWalker {
	  def walk(e: Expression) {
		e match {
		  case ex@ Stop() => {
			  enter(ex)
			  leave(ex)
		  }
		  case ex@ Constant(a) => {
			  enter(ex)
			  leave(ex)
		  }
		  case ex@ Variable(i) => {
			  enter(ex)
			  leave(ex)
		  }
		  case ex@ Call(target, args, typeArgs) => {
			  enter(ex)
			  walk(target)
			  for(a <- args) walk(a)
			  leave(ex)
		  }
		  case ex@ Parallel(f, g) => {
			  enter(ex)
		 	  walk(f)
		 	  walk(g)
			  leave(ex)
		  }
		  case ex@ Sequence(f, g) => {
			  enter(ex)
		 	  walk(f)
		 	  walk(g)
			  leave(ex)
		  }
		  case ex@ Prune(f, g) => {
			  enter(ex)
		 	  walk(f)
		 	  walk(g)
			  leave(ex)
		  } 
		  case ex@ Otherwise(f, g) => {
			  enter(ex)
		 	  walk(f)
		 	  walk(g)
			  leave(ex)
		  } 
		  case ex@ DeclareDefs(defs, body) => {
			  enter(ex)
		 	  for(d <- defs) {
		 	 	  enter(d)
		 	 	  walk(d.body)
		 	 	  leave(d)
		 	  }
		 	  walk(body)
			  leave(ex)
		  } 
		  case ex@ HasType(body,typ) => {
			  enter(ex)
		 	  walk(body)
			  leave(ex)
		  } 
		}
	  }
	  
	  /* 
	   * Enter and Leave methods for specific expressions.
	   * The default behaviour does nothing. Walkers should
       * override these methods as desired.
       */
	  def enter(e: Stop) {}
	  def leave(e: Stop) {}
	  
	  def enter(e: Constant) {}
	  def leave(e: Constant) {}
	  
	  def enter(e: Variable) {}
	  def leave(e: Variable) {}
	  
	  def enter(e: Call) {}
	  def leave(e: Call) {}
	   
	  def enter(e: Parallel) {}
	  def leave(e: Parallel) {}
	  
	  def enter(e: Sequence) {}
	  def leave(e: Sequence) {}
	  
	  def enter(e: Prune) {}
	  def leave(e: Prune) {}
	   
	  def enter(e: Otherwise) {}
	  def leave(e: Otherwise) {}
	  
	  def enter(e: DeclareDefs) {}
	  def leave(e: DeclareDefs) {}
	   
	  def enter(e: HasType) {}
	  def leave(e: HasType) {}
	   
	  def enter(d: Def) {}
	  def leave(d: Def) {}
  	}
}
