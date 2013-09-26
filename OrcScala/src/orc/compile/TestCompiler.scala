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

/**
  * A little compiler driver PURELY for testing. It has some awful hacks to avoid having to duplicate code.
  * @author amp
  */
class TestCompiler extends PhasedOrcCompiler[orc.run.porc.Expr]
  with StandardOrcCompilerEnvInterface[orc.run.porc.Expr]
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
  
  def optimize = new CompilerPhase[CompilerOptions, Expression, Expression] {
    import orc.compile.optimize.named._
    val phaseName = "optimize"
    override def apply(co: CompilerOptions) = { ast =>
      println("====================================== optimizing" )
   
      def opt(prog : Expression, pass : Int) : Expression = {
        import orc.ast.oil.named._
        //println("--------------------- pass " + pass )
        val stats = Map(
            "limits" -> Analysis.count(prog, {
              case Limit(_) => true
              case _ => false
            }),
            "late-binds" -> Analysis.count(prog, {
              case f < x <| g => true
              case _ => false
            }),
            "stops" -> Analysis.count(prog, {
              case Stop() => true
              case _ => false
            }),
            "parallels" -> Analysis.count(prog, {
              case f || g => true
              case _ => false
            }),
            "sequences" -> Analysis.count(prog, {
              case f > x > g => true
              case _ => false
            }),
            "otherwises" -> Analysis.count(prog, {
              case f ow g => true
              case _ => false
            }),
            "nodes" -> Analysis.count(prog, (_ => true)),
            "cost" -> Analysis.cost(prog)
          )
        val s = stats.map(p => s"${p._1} = ${p._2}").mkString(", ")
        Logger.info(s"Orc5C Optimization Pass $pass: $s")
        val analyzer = new ExpressionAnalyzer
        //println(new PrettyPrintWithAnalysis(analyzer).reduce(prog))
        //println("----------------")
        val opts = Optimizer.basicOpts ++ (if( pass > 1 ) Optimizer.secondOpts else List())
        val prog1 = Optimizer(opts)(prog, analyzer)
        Logger.fine(s"analyzer.size = ${analyzer.cache.size}")
        if(prog1 == prog && pass > 1)
          prog1
        else {
          opt(prog1, pass+1)
        }
      }
      
      opt(ast, 1)
    }
  }
  
  val translatePorc = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.porc.Expr] {
    import orc.ast.oil.named._
    val phaseName = "translate5C"
    override def apply(co: CompilerOptions) = { ast => TranslateToPorc.orc5cToPorc(ast) }
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
  
  def optimizePorc = new CompilerPhase[CompilerOptions, orc.ast.porc.Expr, orc.ast.porc.Expr] {
    import orc.ast.porc._
    val phaseName = "optimize"
    override def apply(co: CompilerOptions) = { ast =>
      println("====================================== optimizing" )
   
      def opt(prog : Expr, pass : Int) : Expr = {
        //println("--------------------- pass " + pass )
        val stats = Map(
            "forces" -> Analysis.count(prog, _.isInstanceOf[Force]),
            "spawns" -> Analysis.count(prog, _.isInstanceOf[Spawn]),
            "closures" -> Analysis.count(prog, _.isInstanceOf[Let]),
            "sites" -> Analysis.count(prog, _.isInstanceOf[Site]),
            "nodes" -> Analysis.count(prog, (_ => true)),
            "cost" -> Analysis.cost(prog)
          )
        val s = stats.map(p => s"${p._1} = ${p._2}").mkString(", ")
        orc.ast.porc.Logger.info(s"Porc Optimization Pass $pass: $s")
        /*println("-------==========")
        println(prog)
        println("-------==========")
        */
        val analyzer = new Analyzer
        val prog1 = Optimizer(Optimizer.opts)(prog, analyzer)
        orc.ast.porc.Logger.fine(s"analyzer.size = ${analyzer.cache.size}")
        if(prog1 == prog && pass > 1)
          prog1
        else {
          opt(prog1, pass+1)
        }
      }
      
      opt(ast, 1)
    }
  }
  
  val translatePorcEval = new CompilerPhase[CompilerOptions, orc.ast.porc.Expr, orc.run.porc.Expr] {
    val phaseName = "translatePorcEval"
    override def apply(co: CompilerOptions) = { ast => TranslateToPorcEval(ast) }
  }
  val evalPorc = new CompilerPhase[CompilerOptions, orc.run.porc.Expr, orc.run.porc.Expr] {
    import orc.run.porc._
    val phaseName = "evalPorc"
    override def apply(co: CompilerOptions) = { ast => 
      val interp = new Interpreter()
      interp.start(ast)
      ast
    }
  }
  
  
  override val phases =
    parse.timePhase >>>
    translate.timePhase >>>
    vClockTrans.timePhase >>>
    noUnboundVars.timePhase >>>
    fractionDefs.timePhase >>>
    typeCheck.timePhase >>>
    noUnguardedRecursion.timePhase >>>
    outputIR(1) >>>
    splitPrune.timePhase >>>
    removeUnusedDefs.timePhase >>>
    removeUnusedTypes.timePhase >>>
    outputIR(2) >>>
    optimize >>>
    outputAnalysedAST >>> 
    outputIR(3) >>> 
    translatePorc >>> 
    //porcAnalysis >>>
    outputIR(4) >>>
    optimizePorc >>>
    outputIR(5) >>>
    translatePorcEval >>>
    //outputAST >>>
    evalPorc
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