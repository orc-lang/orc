//
// API.scala -- Scala traits OrcCompilerAPI, OrcAPI, and TokenAPI
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

abstract trait OrcCompilerAPI {
  
  import orc.error.compiletime.CompileLogger
  import scala.util.parsing.input.Reader

  def apply(source: Reader[Char], options: OrcOptions): Expression 
  def apply(source: java.io.Reader, options: OrcOptions): Expression

  def refineOil(oilAstRoot: Expression): Expression = oilAstRoot

//  def progress: ProgressListener
  def compileLogger: CompileLogger
//  def openInclude(includeFileName: String, relativeToFileName: String): java.io.Reader 
//  def loadClass(className: String): Class[_]

}

trait TokenAPI {

  def publish(v : Value): Unit
  def halt: Unit

  def kill: Unit
  def run: Unit

  def printToStdout(s: String): Unit

}

trait OrcAPI {

  type Token <: TokenAPI

//  def start(e: Expression) : Unit
//  def pause
//  def resume
//  def stop

  def emit(v: Value): Unit
  def halted: Unit
  def invoke(t: Token, s: Site, vs: List[Value]): Unit
  def expressionPrinted(s: String): Unit
  def schedule(ts: List[Token]): Unit

  // Schedule function is overloaded for convenience
  def schedule(t: Token) { schedule(List(t)) }
  def schedule(t: Token, u: Token) { schedule(List(t,u)) }

//  def loadClass(className: String): Class[_]
}
