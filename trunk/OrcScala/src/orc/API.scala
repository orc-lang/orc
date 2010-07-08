//
// API.scala -- Scala traits OrcCompilerAPI, CompilerEnvironmentIfc, OrcExecutionAPI, OrcExecutionEnvironmentIfc, and TokenAPI
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

import orc.oil.nameless.Expression
import orc.values.Value
import orc.error.compiletime.CompileLogger
import orc.error.OrcException
import scala.util.parsing.input.Reader

/**
 * The interface from a caller to the Orc compiler
 */
trait OrcCompilerAPI {
  def apply(source: Reader[Char], options: OrcOptions): Expression 
  def apply(source: java.io.Reader, options: OrcOptions): Expression

  def refineOil(oilAstRoot: Expression): Expression = oilAstRoot
}

/**
 * The interface from the Orc compiler to its environment
 */
trait CompilerEnvironmentIfc { 
//  def progress: ProgressListener
  def compileLogger: CompileLogger
  def openInclude(includeFileName: String, relativeToFileName: String, options: OrcOptions): java.io.Reader
//  def loadClass(className: String): Class[_]
}


/**
 * Events reported by an Orc execution
 */
trait OrcEvent
case class Publication(value: AnyRef) extends OrcEvent
case object Halted extends OrcEvent


/**
 * The interface from a caller to an Orc execution
 */
trait OrcExecutionAPI {
  type Token <: TokenAPI

  def run(e: Expression, k: OrcEvent => Unit): Unit
  
  /* Wait for execution to complete, rather than dispatching asynchronously.
   * The continuation takes only values, not events.
   */
  def runSynchronous(node: Expression, k: AnyRef => Unit) {
    val done: scala.concurrent.SyncVar[Unit] = new scala.concurrent.SyncVar()
    def ksync(event: OrcEvent): Unit = {
      event match {
        case orc.Publication(v) => k(v)
        case orc.Halted => { done.set({}) }
      }
    }
    this.run(node, ksync)
    done.get
  }
  
  /* If no continuation is given, discard published values and run silently to completion. */
  def runSynchronous(node: Expression) {
    runSynchronous(node, { v: AnyRef => { /* suppress publications */ } })
  }
//  def pause
//  def resume
  
  /* Shut down this runtime and all of its backing threads.
   * All executions stop without cleanup, though they are not guaranteed to stop immediately. 
   * This will cause all synchronous executions to hang. 
   */
  // TODO: Implement cleaner alternatives.
  def stop: Unit

  def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]): Unit
  def expressionPrinted(s: String): Unit
  def caught(e: Throwable): Unit
  def schedule(ts: List[Token]): Unit

  // Schedule function is overloaded for convenience
  def schedule(t: Token) { schedule(List(t)) }
  def schedule(t: Token, u: Token) { schedule(List(t,u)) }
  //TODO: Move some to the methods above to OrcExecutionEnvironmentIfc
}

/**
 * The interface from an Orc execution machine to its environment
 */
trait OrcExecutionEnvironmentIfc {
//  def loadClass(className: String): Class[_]
}

/**
 * The interface from the environment to tokens of an Orc execution
 */
trait TokenAPI {

  def publish(v : AnyRef): Unit
  def halt: Unit
  def !!(e: OrcException): Unit 
  
  val runtime: OrcExecutionAPI
  
  // TODO: Move these into specialized versions of the runtime
  def printToStdout(s: String): Unit
  def getTimer: java.util.Timer 
}
