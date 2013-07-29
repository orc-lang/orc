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
import orc.ast.oil.named.orc5c.Expression
import orc.ast.porc.TranslateToPorcEval

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
  
  def traversalTest[A] = new CompilerPhase[CompilerOptions, Expression, Expression] {
    val phaseName = "traversalTest"
    override def apply(co: CompilerOptions) = { ast =>
      import orc.ast.oil.named.orc5c._
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
    import orc.ast.oil.named.orc5c._
    val phaseName = "outputAnalysedAST"
    override def apply(co: CompilerOptions) = { ast =>
      println("====================================== Analysed" )
      val analyzer = new ExpressionAnalyzer
      println(new PrettyPrintWithAnalysis(analyzer).reduce(ast in TransformContext()))
      ast
    }
  }
  
  def optimize = new CompilerPhase[CompilerOptions, Expression, Expression] {
    val phaseName = "optimize"
    override def apply(co: CompilerOptions) = { ast =>
      println("====================================== optimizing" )
   
      def opt(prog : Expression, pass : Int) : Expression = {
        import orc.ast.oil.named.orc5c._
        println("--------------------- pass " + pass )
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
        println(stats.map(p => s"${p._1} = ${p._2}").mkString(", "))
        println("-------")
        val analyzer = new ExpressionAnalyzer
        //println(new PrettyPrintWithAnalysis(analyzer).reduce(prog))
        //println("----------------")
        val opts = Optimizer.basicOpts ++ (if( pass > 1 ) Optimizer.secondOpts else List())
        val prog1 = Optimizer(opts)(prog, analyzer)
        println(s"analyzer.size = ${analyzer.cache.size}")
        if(prog1 == prog && pass > 1)
          prog1
        else {
          opt(prog1, pass+1)
        }
      }
      
      opt(ast, 1)
    }
  }
  
  val translate5C = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.oil.named.orc5c.Expression] {
    import orc.ast.oil.named.orc5c._
    val phaseName = "translate5C"
    override def apply(co: CompilerOptions) = { ast => NamedToOrc5C.namedToOrc5C(ast, Map(), Map()) }
  }
  val translatePorc = new CompilerPhase[CompilerOptions, orc.ast.oil.named.orc5c.Expression, orc.ast.porc.Expr] {
    import orc.ast.oil.named.orc5c._
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
        println("--------------------- pass " + pass )
        val stats = Map(
            "forces" -> Analysis.count(prog, _.isInstanceOf[Force]),
            "spawns" -> Analysis.count(prog, _.isInstanceOf[Spawn]),
            "closures" -> Analysis.count(prog, _.isInstanceOf[Let]),
            "sites" -> Analysis.count(prog, _.isInstanceOf[Site]),
            "nodes" -> Analysis.count(prog, (_ => true)),
            "cost" -> Analysis.cost(prog)
          )
        println(stats.map(p => s"${p._1} = ${p._2}").mkString(", "))
        /*println("-------==========")
        println(prog)
        println("-------==========")
        */
        val analyzer = new Analyzer
        val prog1 = Optimizer(Optimizer.opts)(prog, analyzer)
        println(s"analyzer.size = ${analyzer.cache.size}")
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
    //traversalTest >>>
    optimize >>>
    outputAnalysedAST >>> 
    outputAST >>> 
    translatePorc >>> 
    //porcAnalysis >>>
    outputAST >>>
    //optimizePorc >>>
    //outputAST >>>
    translatePorcEval >>>
    outputAST >>>
    evalPorc >>>
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