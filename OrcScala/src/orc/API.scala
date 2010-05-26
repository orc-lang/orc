//
// API.scala -- Scala objects OrcAPI and TokenAPI
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

trait TokenAPI {
	
	import oil.Value
	
	def publish(v : Value): Unit
	def halt: Unit
	
	def kill: Unit
	def run: Unit
}

trait OrcAPI {
	
	import oil._
	
	type Token <: TokenAPI
	
//	def start(e: Expression) : Unit
//	def pause
//	def resume
//	def stop
	
	def emit(v: Value): Unit
	def halted: Unit
	def invoke(t: Token, s: Site, vs: List[Value]): Unit
	def schedule(ts: List[Token]): Unit
	
	// Schedule function is overloaded for convenience
	def schedule(t: Token) { schedule(List(t)) }
	def schedule(t: Token, u: Token) { schedule(List(t,u)) }
}

