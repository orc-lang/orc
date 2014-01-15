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

class PorcOrcCompiler extends PhasedOrcCompiler[(orc.run.porc.Expr, orc.run.porc.PorcDebugTable)]
  with StandardOrcCompilerEnvInterface[(orc.run.porc.Expr, orc.run.porc.PorcDebugTable)]
  with CoreOrcCompilerPhases {
  
  def traversalTest[A] = new CompilerPhase[CompilerOptions, Expression, Expression] {
    val phaseName = "traversalTest"
    override def apply(co: CompilerOptions) = { ast =>
      import orc.ast.oil.named._
      /*val ts = List(
        new ContextualTransform.Pre {
          override def onExpression(implicit ctx: TransformContext) = {
            case e @ Limit(_) => println(s"$e in $ctx"); e
          }
        },
        new ContextualTransform.NonDescending {
          override def onExpression(implicit ctx: TransformContext) = {
            case e @ Limit(_) => println(s"$e in $ctx"); HasType(e, Bot())
          }
        },
        new ContextualTransform.Post {
          override def onExpression(implicit ctx: TransformContext) = {
            case e @ Limit(_) => println(s"$e in $ctx"); HasType(e, Bot())
          }
        }
        )*/
      object boundTo {
        def unapply(v : BoundVar)(implicit ctx: TransformContext): Option[(BoundVar, Bindings.Binding)] = {
          if(ctx.contains(v)) {
            Some((v, ctx(v)))
          } else
            None
        }
      }
      val ts = List(
        new ContextualTransform.Pre {
          override def onArgument(implicit ctx: TransformContext) = {
            case e@(v boundTo b) => println(s"$v $b ${ctx.size}"); e
          }
        }
        )
      for(t <- ts) {
        println(s"====================== $t")
        t(ast)
      }
      ast
    }
  }
  
  def outputAnalysedAST = new CompilerPhase[CompilerOptions, Expression, Expression] {
    import orc.compile.optimize.named._
    import orc.ast.oil.named._
    val phaseName = "outputAnalysedAST"
    override def apply(co: CompilerOptions) = { ast =>
      println("====================================== Analysed" )
      val analyzer = new ExpressionAnalyzer
      println(new PrettyPrintWithAnalysis(analyzer).reduce(ast in TransformContext()))
      ast
    }
  }
    
  val translatePorc = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.porc.Expr] {
    import orc.ast.oil.named._
    val phaseName = "translate5C"
    override def apply(co: CompilerOptions) = { ast => 
      val e = TranslateToPorc.orc5cToPorc(ast)
      e.assignNumbers()
      e
    }
  }
  
  /*val porcAnalysis = new CompilerPhase[CompilerOptions, orc.ast.porc.Expr,orc.ast.porc.Expr] {
    import orc.ast.porc._
    val phaseName = "translate5C"
    override def apply(co: CompilerOptions) = { ast => 
      val analyzer = new Analyzer()
      import analyzer.ImplicitResults._
      (new ContextualTransform.Pre {
        override def onExpr = {
          case c => {
            println(c.immediatelyCallsSet + c.e.toString.take(100).replace('\n', ' '))
            c.e
          }
        }
      })(ast)
      ast
    }
  }*/
  
  val optimizePorc = new CompilerPhase[CompilerOptions, orc.ast.porc.Expr, orc.ast.porc.Expr] {
    import orc.ast.porc._
    val phaseName = "optimize"
    override def apply(co: CompilerOptions) = { ast =>
      val maxPasses = co.options.optimizationFlags("porc:max-passes").asInt(5)
   
      def opt(prog : Expr, pass : Int) : Expr = {
        val analyzer = new Analyzer
        val stats = Map(
            "forces" -> Analysis.count(prog, _.isInstanceOf[Force]),
            "spawns" -> Analysis.count(prog, _.isInstanceOf[Spawn]),
            "closures" -> Analysis.count(prog, _.isInstanceOf[Let]),
            "sites" -> Analysis.count(prog, _.isInstanceOf[Site]),
            "nodes" -> Analysis.count(prog, (_ => true)),
            "cost" -> analyzer(prog in TransformContext()).cost
          )
        val s = stats.map(p => s"${p._1} = ${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Porc Optimization Pass $pass: $s")
        //println("-------==========")
        //println(prog)
        //println("-------==========")
        
        val prog1 = Optimizer(co)(prog, analyzer)
        orc.ast.porc.Logger.fine(s"analyzer.size = ${analyzer.cache.size}")
        if(prog1 == prog || pass > maxPasses)
          prog1
        else {
          opt(prog1, pass+1)
        }
      }
      
      val e = if(co.options.optimizationFlags("porc").asBool())
        opt(ast, 1)
      else
        ast
      
      e.assignNumbers()
      e
    }
  }
  
  val translatePorcEval = new CompilerPhase[CompilerOptions, orc.ast.porc.Expr, (orc.run.porc.Expr, orc.run.porc.PorcDebugTable)] {
    val phaseName = "translatePorcEval"
    override def apply(co: CompilerOptions) = { ast => 
      TranslateToPorcEval(ast)
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
      removeUnusedTypes.timePhase >>>
      outputIR(3) >>>
      //outputAnalysedAST >>>
      translatePorc.timePhase >>>
      outputIR(4) >>>
      optimizePorc.timePhase >>>
      outputIR(5) >>>
      translatePorcEval
}

object PorcOrcCompiler {
  import orc.Main._
  
  def main(args: Array[String]) {
      try {
      val options = new OrcCmdLineOptions()
      options.parseCmdLine(args)

      setupLogging(options)

      val compiler = new PorcOrcCompiler()

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