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
import orc.ast.orctimizer.named.AssertedType

/** StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces
  * and specifies that compilation will finish with named.
  *
  * @author jthywiss
  */
abstract class OrctimizerOrcCompiler() extends PhasedOrcCompiler[porc.MethodCPS]
  with StandardOrcCompilerEnvInterface[porc.MethodCPS]
  with CoreOrcCompilerPhases {

  private[this] var currentAnalysisCache: Option[AnalysisCache] = None   
  def cache = currentAnalysisCache match {
    case Some(c) => c
    case None =>
      currentAnalysisCache = Some(new AnalysisCache())
      currentAnalysisCache.get
  }
  def clearCache() = currentAnalysisCache = None
  
  def clearCachePhase[T] = new CompilerPhase[CompilerOptions, T, T] {
    val phaseName = "clearCache"
    override def apply(co: CompilerOptions) =
      { ast =>
        clearCache()
        ast
      }
  }

  Logger.warning("You are using the Porc/Orctimizer back end. It is UNSTABLE!!")

  val toOrctimizer = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orctimizer.named.Expression] {
    val phaseName = "to-orctimizer"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OILToOrctimizer(co)
        val res = translator(ast)(translator.Context(Map(), Map()))
        orctimizer.named.VariableChecker(res.toZipper(), co)
        res
      }
  }

  def optimize(pred: (CompilerOptions) => Boolean = (co) => true) = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, orctimizer.named.Expression] {
    import orctimizer.named._
    val phaseName = "orctimizer-optimize"
    override def apply(co: CompilerOptions) = { ast =>
      //println(co.options.optimizationFlags)
      val maxPasses = co.options.optimizationFlags("orct:max-passes").asInt(8)
      val optimizer = StandardOptimizer(co)

      def opt(prog: Expression, pass: Int): Expression = {
        def logAnalysis() = {
          val typeCounts: collection.Map[Class[_], Int] = {
            val counts = new collection.mutable.HashMap[Class[_], Int]()
            def add(c: Class[_]) = {
              counts += c -> (counts.getOrElse(c, 0) + 1)
            }
            def process(n: NamedAST): Unit = {
              add(n.getClass())
              n.subtrees foreach process
            }
            process(prog)
            counts
          }
          val stats = typeCounts
                .filterNot(kv => classOf[Type].isAssignableFrom(kv._1) || 
                    classOf[DeclareMethods].isAssignableFrom(kv._1) || 
                    classOf[HasType].isAssignableFrom(kv._1) ||
                    classOf[AssertedType].isAssignableFrom(kv._1) ||
                    classOf[Var].isAssignableFrom(kv._1) ||
                    classOf[DeclareType].isAssignableFrom(kv._1))
                .map({ case (k, v) => (k.getSimpleName(), v) }) + 
                ("total" -> typeCounts.values.sum)
          val s = stats.map(p => s"${p._1}=${p._2}").mkString(", ")
          s"Orctimizer before pass $pass/$maxPasses: $s"
        }

        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, logAnalysis())

        {
          lazy val z = prog.toZipper()
          lazy val cg = cache.get(CallGraph)(z, None)
          //cg.debugShow()
        }


        val prog1 = optimizer(prog.toZipper(), cache)

        def optimizationCountsStr = optimizer.optimizationCounts.map(p => s"${p._1}=${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Orctimizer after pass $pass/$maxPasses: $optimizationCountsStr")

        orctimizer.named.VariableChecker(prog1.toZipper(), co)

        if (prog1 == prog || pass >= maxPasses)
          prog1
        else {
          opt(prog1, pass + 1)
        }
      }

      def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"

      {
        val z = ast.toZipper()

        lazy val fg = cache.get(FlowGraph)(z, None)
        lazy val cg = cache.get(CallGraph)(z, None)
        lazy val pubs = cache.get(PublicationCountAnalysis)(z, None)
        lazy val delay = cache.get(DelayAnalysis)(z, None)
        lazy val effect = cache.get(EffectAnalysis)(z, None)
        lazy val forces = cache.get(ForceAnalysis)(z, None)

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

        //println("=============== force results ---")
        //println(forces.results.par.map(p => s"${shortString(p._1.value)}\t----=========--> ${p._2}").seq.mkString("\n"))
        //forces.debugShow()

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
  val toPorc = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, porc.MethodCPS] {
    val phaseName = "to-porc"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OrctimizerToPorc(co)
        val res = translator(ast, cache)
        porc.VariableChecker(res.toZipper(), co)
        res
      }
  }

  val optimizePorc = new CompilerPhase[CompilerOptions, porc.MethodCPS, porc.MethodCPS] {
    import orc.ast.porc._
    val phaseName = "porc-optimize"
    override def apply(co: CompilerOptions) = { ast =>
      val maxPasses = co.options.optimizationFlags("porc:max-passes").asInt(5)
      val analyzer = new Analyzer
      val optimizer = Optimizer(co)

      def opt(prog: MethodCPS, pass: Int): MethodCPS = {
        val stats = Map(
          "forces" -> Analysis.count(prog, _.isInstanceOf[Force]),
          "spawns" -> Analysis.count(prog, _.isInstanceOf[Spawn]),
          "closures" -> Analysis.count(prog, _.isInstanceOf[Continuation]),
          "indirect calls" -> Analysis.count(prog, _.isInstanceOf[MethodCPSCall]),
          "direct calls" -> Analysis.count(prog, _.isInstanceOf[MethodDirectCall]),
          "sites" -> Analysis.count(prog, _.isInstanceOf[MethodDeclaration]),
          "nodes" -> Analysis.count(prog, (_ => true)),
          "cost" -> Analysis.cost(prog))
        def s = stats.map(p => s"${p._1} = ${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Porc optimization pass $pass/$maxPasses: $s")

        val prog1 = optimizer(prog, analyzer).asInstanceOf[MethodCPS]

        def optimizationCountsStr = optimizer.optimizationCounts.map(p => s"${p._1} = ${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Porc optimization pass $pass/$maxPasses: $optimizationCountsStr")
        porc.VariableChecker(prog1.toZipper(), co)

        if (prog1 == prog || pass >= maxPasses)
          prog1
        else {
          opt(prog1, pass + 1)
        }
      }

      val e = if (co.options.optimizationFlags("porc").asBool())
        opt(ast, 1)
      else
        ast

      porc.Statistics(e.body)
        
      e
    }
  }

  
  val indexPorc = new CompilerPhase[CompilerOptions, porc.MethodCPS, porc.MethodCPS] {
    import orc.ast.porc._
    val phaseName = "porc-index"
    override def apply(co: CompilerOptions) = { ast =>
      IndexAST(ast)
      ast
    }
  }
  
  def nullOutput[T >: Null] = new CompilerPhase[CompilerOptions, T, T] {
    val phaseName = "nullOutput"
    override def apply(co: CompilerOptions) =
      { ast =>
        null
      }
  }

  ////////
  // Compose phases into a compiler
  ////////
  val phases =
    parse.timePhase >>>
      outputIR(1) >>>
      translate.timePhase >>>
      vClockTrans.timePhase >>>
      noUnboundVars.timePhase >>>
      fractionDefs.timePhase >>>
      typeCheck.timePhase >>>
      noUnguardedRecursion.timePhase >>>
      outputIR(2) >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      outputIR(3) >>>
      abortOnError >>>
      toOrctimizer.timePhase >>>
      outputIR(4) >>>
      optimize().timePhase >>>
      outputIR(5) >>>
      //unroll >>>
      //outputIR(6, _.options.optimizationFlags("orct:unroll-def").asBool()) >>>
      //optimize(_.options.optimizationFlags("orct:unroll-def").asBool()) >>>
      //outputIR(7, _.options.optimizationFlags("orct:unroll-def").asBool()) >>>
      toPorc.timePhase >>>
      clearCachePhase >>>
      outputIR(8) >>>
      optimizePorc.timePhase >>>
      indexPorc.timePhase >>>
      outputIR(9)
}
