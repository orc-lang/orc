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
import orc.values.sites.Site
import orc.error.compiletime.CompileLogger;
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
 * The interface from a caller to an Orc execution machine 
 */
trait OrcExecutionAPI {
  type Token <: TokenAPI

//  def start(e: Expression) : Unit
//  def pause
//  def resume
//  def stop

  def emit(v: Value): Unit
  def halted: Unit
  def invoke(t: Token, s: Site, vs: List[Value]): Unit
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
 * The interface from an Orc execution machine to tokens
 */
trait TokenAPI {
  def publish(v : Value): Unit
  def halt: Unit

  def kill: Unit
  def run: Unit

  def printToStdout(s: String): Unit
  def getTimer: java.util.Timer 
}
