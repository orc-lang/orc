//
// OrctimizerOrcCompiler.scala -- Scala class OrctimizerOrcCompiler
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.compile._
import orc.ast.orctimizer
import orc.ast.porc
import orc.error.compiletime.CompileLogger
import orc.compile.tojava.PorcToJava

/** StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces
  * and specifies that compilation will finish with named.
  *
  * @author jthywiss
  */
abstract class OrctimizerOrcCompiler() extends PhasedOrcCompiler[String]
  with StandardOrcCompilerEnvInterface[String]
  with CoreOrcCompilerPhases {

  Logger.warning("You are using the Porc/Orctimizer back end. It is UNSTABLE!!")

  val toOrctimizer = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orctimizer.named.Expression] {
    val phaseName = "translate"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OILToOrctimizer()
        translator(ast)(Map())
      }
  }

  def optimize(pred: (CompilerOptions) => Boolean = (co) => true) = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, orctimizer.named.Expression] {
    import orctimizer.named._
    val phaseName = "optimize"
    override def apply(co: CompilerOptions) = { ast =>
      //println(co.options.optimizationFlags)
      val maxPasses = co.options.optimizationFlags("orct:max-passes").asInt(8)
      val cache = new AnalysisCache()
      val optimizer = StandardOptimizer(co)

      def opt(prog: Expression, pass: Int): Expression = {
        val prog1 = optimizer(prog.toZipper(), cache)

        def optimizationCountsStr = optimizer.optimizationCounts.map(p => s"${p._1} = ${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Optimizer pass $pass/$maxPasses: $optimizationCountsStr")

        if ((prog1 == prog && pass > 1) || pass > maxPasses)
          prog1
        else {
          opt(prog1, pass + 1)
        }
      }
      /*
      def opt(prog: Expression, pass: Int): Expression = {
        def logAnalysis() = {
          val stats = Map(
            "limits" -> Analysis.count(prog, {
              case Trim(_) => true
              case _ => false
            }),
            "futures" -> Analysis.count(prog, {
              case Future(_) => true
              case _ => false
            }),
            "forces" -> Analysis.count(prog, {
              case Force(_, _, _, _) => true
              case _ => false
            }),
            "ifdef" -> Analysis.count(prog, {
              case IfDef(_, _, _) => true
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
              case f Otherwise g => true
              case _ => false
            }),
            "nodes" -> Analysis.count(prog, (_ => true)),
            "cost" -> Analysis.cost(prog))
          val s = stats.map(p => s"${p._1} = ${p._2}").mkString(", ")
          s"Orctimizer before pass $pass/$maxPasses: $s"
        }

        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, logAnalysis())

        val analyzer = new ExpressionAnalyzer
        //val optimizer = StandardOptimizer(co)
        val prog1 = ??? // optimizer(prog, analyzer)

        //def optimizationCountsStr = optimizer.optimizationCounts.map(p => s"${p._1} = ${p._2}").mkString(", ")
        //co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Optimizer pass $pass/$maxPasses: $optimizationCountsStr")

        if ((prog1 == prog && pass > 1) || pass > maxPasses)
          prog1
        else {
          opt(prog1, pass + 1)
        }
      }
      */

      def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"

      {
        //val g = new FlowGraph(ast)
        /*println("=============== Nodes ---")
        println(g.nodes.mkString("\n"))
        println("=============== Edges ---")
        println(g.edges.mkString("\n"))
        */
        //println(g.toDot)
        //g.debugShow()
        
        val z = ast.toZipper()

        lazy val fg = cache.get(FlowGraph)(z, None)
        lazy val cg = cache.get(CallGraph)(z, None)
        lazy val pubs = cache.get(PublicationCountAnalysis)(z, None)
        lazy val delay = cache.get(DelayAnalysis)(z, None)
        lazy val effect = cache.get(EffectAnalysis)(z, None)

        //fg.debugShow()

        //println("=============== results ---")
        //println(cg.results.filter(p => p._1.ast.isInstanceOf[Var]).map(p => s"${shortString(p._1)}\t----> ${p._2}").mkString("\n"))

        //cg.debugShow()

        //println("=============== publication results ---")
        //println(pubs.expressions.par.map(p => s"${shortString(p._1.ast)}\t----=========--> ${p._2}").seq.mkString("\n"))
        //pubs.debugShow()

        //println("=============== delay results ---")
        //println(delay.results.par.map(p => s"${shortString(p._1.ast)}\t----=========--> ${p._2}").seq.mkString("\n"))
        //delay.debugShow()

        //effect.debugShow()

        //System.exit(0)
      }

      val e = if (co.options.optimizationFlags("orct").asBool() && pred(co))
        opt(ast, 1)
      else
        ast

      e
    }
  }
  lazy val unroll = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, orctimizer.named.Expression] {
    import orctimizer.named._
    val phaseName = "unroll"
    override def apply(co: CompilerOptions) = { ast =>
      //println(co.options.optimizationFlags)
      val maxPasses = co.options.optimizationFlags("orct:unroll-repeats").asInt(2)

      /*
      def opt(prog: Expression, pass: Int): Expression = {
        val analyzer = new ExpressionAnalyzer
        val optimizer = UnrollOptimizer(co)
        val prog1 = optimizer(prog, analyzer)

        def optimizationCountsStr = optimizer.optimizationCounts.map(p => s"${p._1} = ${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Optimizer unroll pass $pass/$maxPasses: $optimizationCountsStr")

        if ((prog1 == prog && pass > 1) || pass > maxPasses)
          prog1
        else {
          opt(prog1, pass + 1)
        }
      }

      val e = if (co.options.optimizationFlags("orct").asBool() &&
        co.options.optimizationFlags("orct:unroll-def").asBool())
        opt(ast, 1)
      else
        ast
        */

      ast
    }
  }
}

class PorcOrcCompiler() extends OrctimizerOrcCompiler {
  val toPorc = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, porc.DefCPS] {
    val phaseName = "translate"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OrctimizerToPorc()
        translator(ast)
      }
  }

  val porcToJava = new CompilerPhase[CompilerOptions, porc.DefCPS, String] {
    val phaseName = "toJava"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new PorcToJava()
        translator(ast)
      }
  }

  val optimizePorc = new CompilerPhase[CompilerOptions, porc.DefCPS, porc.DefCPS] {
    import orc.ast.porc._
    val phaseName = "optimize"
    override def apply(co: CompilerOptions) = { ast =>
      val maxPasses = co.options.optimizationFlags("porc:max-passes").asInt(5)

      def opt(prog: DefCPS, pass: Int): DefCPS = {
        val analyzer = new Analyzer
        val stats = Map(
          "forces" -> Analysis.count(prog, _.isInstanceOf[Force]),
          "spawns" -> Analysis.count(prog, _.isInstanceOf[Spawn]),
          "closures" -> Analysis.count(prog, _.isInstanceOf[Continuation]),
          "indirect calls" -> Analysis.count(prog, _.isInstanceOf[SiteCall]),
          "direct calls" -> Analysis.count(prog, _.isInstanceOf[SiteCallDirect]),
          "sites" -> Analysis.count(prog, _.isInstanceOf[DefDeclaration]),
          "nodes" -> Analysis.count(prog, (_ => true)),
          "cost" -> Analysis.cost(prog))
        def s = stats.map(p => s"${p._1} = ${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Porc optimization pass $pass/$maxPasses: $s")
        //println("-------==========")
        //println(prog)
        //println("-------==========")

        val optimizer = Optimizer(co)
        val prog1 = optimizer(prog, analyzer).asInstanceOf[DefCPS]

        orc.ast.porc.Logger.finest(s"analyzer.size = ${analyzer.cache.size}")

        def optimizationCountsStr = optimizer.optimizationCounts.map(p => s"${p._1} = ${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Porc optimization pass $pass/$maxPasses: $optimizationCountsStr")

        if (prog1 == prog || pass > maxPasses)
          prog1
        else {
          opt(prog1, pass + 1)
        }
      }

      val e = if (co.options.optimizationFlags("porc").asBool())
        opt(ast, 1)
      else
        ast

      TransformContext.clear()
      e.assignNumbers()
      e
    }
  }

  def nullOutput[T] = new CompilerPhase[CompilerOptions, T, String] {
    val phaseName = "toJava"
    override def apply(co: CompilerOptions) =
      { ast =>
        ""
      }
  }

  ////////
  // Compose phases into a compiler
  ////////
  val phases =
    parse.timePhase >>>
      translate.timePhase >>>
      vClockTrans.timePhase >>>
      noUnboundVars.timePhase >>>
      fractionDefs.timePhase >>>
      typeCheck.timePhase >>>
      //outputIR(1) >>>
      noUnguardedRecursion.timePhase >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      outputIR(2) >>>
      toOrctimizer >>>
      outputIR(3) >>>
      optimize() >>>
      outputIR(4) >>>
      //unroll >>>
      //outputIR(5, _.options.optimizationFlags("orct:unroll-def").asBool()) >>>
      //optimize(_.options.optimizationFlags("orct:unroll-def").asBool()) >>>
      //outputIR(6, _.options.optimizationFlags("orct:unroll-def").asBool()) >>>
      toPorc >>>
      outputIR(7) >>>
      optimizePorc >>>
      outputIR(8) >>>
      porcToJava >>>
      outputIR(9)
}
