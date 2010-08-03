//
// API.scala -- Interfaces for Orc compiler and runtime
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

import java.io.IOException
import orc.compile.parse.OrcInputContext
import orc.oil.nameless.Expression
import orc.error.compiletime.CompileLogger
import orc.error.OrcException
import orc.error.compiletime.CompilationException
import orc.error.runtime.ExecutionException


/**
 * The interface from a caller to the Orc compiler
 */
trait OrcCompilerProvides {
  @throws(classOf[IOException])
  def apply(source: OrcInputContext, options: OrcOptions, compileLogger: CompileLogger): Expression
  def refineOil(oilAstRoot: Expression): Expression = oilAstRoot
}


/**
 * The interface from the Orc compiler to its environment
 */
trait OrcCompilerRequires { 
//  def progress: ProgressListener
  @throws(classOf[IOException])
  def openInclude(includeFileName: String, relativeTo: OrcInputContext, options: OrcOptions): OrcInputContext
  @throws(classOf[ClassNotFoundException])
  def loadClass(className: String): Class[_]
}


/** 
 * An Orc compiler
 */
trait OrcCompiler extends OrcCompilerProvides with OrcCompilerRequires


/**
 * The interface from a caller to an Orc runtime
 */
trait OrcRuntimeProvides {
  type Token <: TokenAPI 

  @throws(classOf[ExecutionException])
  def run(e: Expression, k: OrcEvent => Unit, options: OrcOptions): Unit
  def stop: Unit
}


/**
 *  The interface from an Orc runtime to its environment
 */
trait OrcRuntimeRequires {
  def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]): Unit
}


/**
 * An Orc runtime 
 */
trait OrcRuntime extends OrcRuntimeProvides with OrcRuntimeRequires {  
  type Token <: TokenAPI
  
  def schedule(ts: List[Token]): Unit
 
  // Schedule function is overloaded for convenience
  def schedule(t: Token) { schedule(List(t)) }
  def schedule(t: Token, u: Token) { schedule(List(t,u)) }
}


/**
 * The interface from the environment to tokens of an Orc runtime
 */
trait TokenAPI {
  def publish(v : AnyRef): Unit
  def halt: Unit
  def !!(e: OrcException): Unit 
  def notify(event: OrcEvent): Unit

  val runtime: OrcRuntime
}


/**
 * An event reported by an Orc execution
 */
//TODO: Move this to be part of an engine's signature
trait OrcEvent
// Not all executions will support all four of these events
case class PublishedEvent(value: AnyRef) extends OrcEvent
case object HaltedEvent extends OrcEvent
case class PrintedEvent(s: String) extends OrcEvent
case class CaughtEvent(e: Throwable) extends OrcEvent
// If this list grows, maybe it should become a sub-package


/**
 * An action for a few major events reported by an Orc execution.
 * This is an alternative to receiving <code>OrcEvents</code> for a client
 * with simple needs, or for Java code that cannot create Scala functions.
 */
class OrcEventAction {
  val asFunction: (OrcEvent => Unit) = _ match {
    case PublishedEvent(v) => published(v)
    case PrintedEvent(s) => printed(s)
    case CaughtEvent(e) => caught(e)
    case HaltedEvent => halted()
    case _ => { }
  }
  
  def published(value: AnyRef) { }
  def printed(output: String) { }
  def caught(e: Throwable) { }
  def halted() { }
}
