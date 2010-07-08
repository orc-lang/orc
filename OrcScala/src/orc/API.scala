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

import orc.oil.nameless.Expression
import orc.error.compiletime.CompileLogger
import orc.error.OrcException
import scala.util.parsing.input.Reader



/**
 * The interface from a caller to the Orc compiler
 */
trait OrcCompilerProvides {
  def apply(source: Reader[Char], options: OrcOptions): Expression 
  def apply(source: java.io.Reader, options: OrcOptions): Expression

  def refineOil(oilAstRoot: Expression): Expression = oilAstRoot
}

/**
 * The interface from the Orc compiler to its environment
 */
trait OrcCompilerRequires { 
//  def progress: ProgressListener
  def compileLogger: CompileLogger
  def openInclude(includeFileName: String, relativeToFileName: String, options: OrcOptions): java.io.Reader
//  def loadClass(className: String): Class[_]
}

/** An Orc compiler */
trait OrcCompiler extends OrcCompilerProvides with OrcCompilerRequires



/** The interface from a caller to an Orc runtime */
trait OrcRuntimeProvides {
  type Token <: TokenAPI 
  
  def run(e: Expression, k: OrcEvent => Unit): Unit
  def stop: Unit
}

/** The interface from an Orc runtime to its environment */
trait OrcRuntimeRequires { 
  def invoke(t: TokenAPI, v: AnyRef, vs: List[AnyRef]): Unit
  def caught(e: Throwable): Unit
}

/** An Orc runtime */
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
  
  val runtime: OrcRuntime
}



/**
 * An event reported by an Orc execution
 */
trait OrcEvent
case class Publication(value: AnyRef) extends OrcEvent
case object Halted extends OrcEvent