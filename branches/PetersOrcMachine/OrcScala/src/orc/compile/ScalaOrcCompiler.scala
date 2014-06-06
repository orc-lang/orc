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
import orc.ast.porc.TranslateToPorcEval
import orc.compile.translate.TranslateToPorc
import orc.ast.oil.named.Expression
import orc.error.compiletime.CompileLogger
import orc.ast.porc.ScalaCodeGen
import orc.run.compiled.ScalaRuntimeCompiler

class ScalaOrcCompiler extends PhasedOrcCompiler[(String, Map[String, AnyRef])]
  with StandardOrcCompilerEnvInterface[(String, Map[String, AnyRef])]
  with CoreOrcCompilerPhases
  with CorePorcCompilerPhases {

  def printScala = new CompilerPhase[CompilerOptions, orc.ast.porc.Expr, orc.ast.porc.Expr] {
    val phaseName = "outputAnalysedAST"
    override def apply(co: CompilerOptions) = { ast =>
      println("====================================== Scala" )
      val proc = new ScalaRuntimeCompiler()
      println(proc(ast))
      val mod = proc.compile(ast)
      println(mod(null, null, null, null))
      ast
    }
  }
  def translateToScala = new CompilerPhase[CompilerOptions, orc.ast.porc.Expr, (String, Map[String, AnyRef])] {
    val phaseName = "outputAnalysedAST"
    override def apply(co: CompilerOptions) = { ast =>
      //println("====================================== Scala" )
      val proc = new ScalaRuntimeCompiler()
      val res = proc(ast)
      res
    }
  }
  
  
  
  override val phases =
    parse.timePhase >>>
      translate.timePhase >>>
      vClockTrans.timePhase >>>
      noUnboundVars.timePhase >>>
      fractionDefs.timePhase >>>
      outputIR(1) >>>
      typeCheck.timePhase >>>
      noUnguardedRecursion.timePhase >>>
      splitPrune.timePhase >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      makeResilientTrans.timePhase >>>
      outputIR(2) >>>
      optimize.timePhase >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      outputIR(3) >>>
      translatePorc.timePhase >>>
      outputIR(4) >>>
      optimizePorc.timePhase >>>
      outputIR(5) >>>
      translateToScala.timePhase >>>
      outputIR(6)
      
}
