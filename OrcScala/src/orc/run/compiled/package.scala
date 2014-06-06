//
// _package.scala -- Scala class/trait/object _package
// Project OrcScala
//
// $Id$
//
// Created by amp on May 19, 2014.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run


/*
 * Types:
 * 
 * Closures come in 2 forms: 
 * native Scala defs (x1, ..., xn) => y, and untyped defs with type (Seq[*]) => *.
 * Then the definition is in scope we use the direct Scala call.
 * Whenever the closure is escaping it is boxed into function taking sequences as the arguments.
 * The sequence taking function is generated as a wrapper around the original once when the original is emitted. 
 * 
 * Site defs are functions that take their arguments and also: C, T, P.
 * Unlike closures they have no direct form.
 * The boxed form replaces the args with a Seq, but has the meta arguments seperate still.
 * 
 * Site is Site
 * 
 * Terminator is similar to porc.Terminator
 * Counter is similar to porc.Counter
 * 
 * Runtime is similar to Interpreter
 * 
 * RuntimeContext handles queuing and is actually the thread object returned by Thread.currentThread()
 * 
 * EvalContext is real scala variables
 * 
 */

import orc.OrcExecutionOptions

/**
  *
  * @author amp
  */
package object compiled {
  type Terminable = porc.Terminable
  
  type Join = porc.Join
  
  type OrcEvent = orc.OrcEvent

  type Future = porc.Future
  val Future = porc.Future
  type Flag = porc.Flag
  
  type Closure = (Seq[AnyRef]) => AnyRef
  
  type OrcModule = (Counter, (OrcEvent) => Unit, OrcExecutionOptions, RuntimeContext) => OrcModuleInstance
}