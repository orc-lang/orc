//
// TestCompiler.scala -- Scala class/trait/object TestCompiler
// Project OrcScala
//
// $Id$
//
// Created by amp on Apr 29, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile

import java.io.FileInputStream
import orc.util.CmdLineUsageException
import orc.util.PrintVersionAndMessageException
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import orc.ast.oil.named.orc5c.NamedToOrc5C
import orc.ast.oil.named.orc5c.Orc5CAST
import orc.ast.oil.named.orc5c.Analyzer
import orc.ast.oil.named.orc5c.PrettyPrint
import orc.ast.oil.named.orc5c.Expression
import orc.ast.oil.named.orc5c.PrettyPrintWithAnalysis

/**
  * A little compiler driver PURELY for testing. It has some awful hacks to avoid having to duplicate code.
  * @author amp
  */
class TestCompiler extends StandardOrcCompiler {
  // Generate XML for the AST and echo it to console; useful for testing.
  def outputAST[A] = new CompilerPhase[CompilerOptions, A, A] {
    val phaseName = "outputAST"
    override def apply(co: CompilerOptions) = { ast =>
      println("====================================== " + ast.getClass )
      println(ast)

      ast
    }
  }
  
  def outputAnalysedAST = new CompilerPhase[CompilerOptions, Expression, Expression] {
    val phaseName = "outputAnalysedAST"
    override def apply(co: CompilerOptions) = { ast =>
      println("====================================== Analysed" )
      val analyzer = new Analyzer()
      analyzer.analyze(ast)
      println(new PrettyPrintWithAnalysis(analyzer).reduce(ast))

      ast
    }
  }
  
  val translate5C = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.oil.named.orc5c.Expression] {
    val phaseName = "translate5C"
    override def apply(co: CompilerOptions) = { ast => NamedToOrc5C.namedToOrc5C(ast, Map(), Map()) }
  }
  
  def awfulHack[A, B >: AnyRef] = new CompilerPhase[CompilerOptions, A, orc.ast.oil.nameless.Expression] {
    val phaseName = "awfulHack"
    override def apply(co: CompilerOptions) = { _ => null }
  }
    
  override val phases =
    parse.timePhase >>>
    translate.timePhase >>>
    vClockTrans.timePhase >>>
    noUnboundVars.timePhase >>>
    fractionDefs.timePhase >>>
    typeCheck.timePhase >>>
    removeUnusedDefs.timePhase >>>
    removeUnusedTypes.timePhase >>>
    noUnguardedRecursion.timePhase >>>
    outputAST >>>
    translate5C.timePhase >>>
    outputAST >>>
    outputAnalysedAST >>>
    awfulHack
}

object TestCompiler {
  import orc.Main._
  
  def main(args: Array[String]) {
      try {
      val options = new OrcCmdLineOptions()
      options.parseCmdLine(args)
      setupLogging(options)

      val compiler = new TestCompiler()

      val stream = new FileInputStream(options.filename)
      val reader = new InputStreamReader(stream, "UTF-8")
      
      compiler(reader, options, new OutputStreamWriter(Console.err))
    } catch {
      case e: CmdLineUsageException => Console.err.println("Orc: " + e.getMessage)
      case e: PrintVersionAndMessageException => println(orcImplName + " " + orcVersion + "\n" + orcURL + "\n" + orcCopyright + "\n\n" + e.getMessage)
      case e: FileNotFoundException => Console.err.println("Orc: File not found: " + e.getMessage)
    }  
  }
}