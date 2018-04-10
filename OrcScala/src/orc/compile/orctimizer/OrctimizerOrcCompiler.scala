//
// OrctimizerOrcCompiler.scala -- Scala class OrctimizerOrcCompiler
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.orctimizer

import orc.compile._
import orc.ast.orctimizer
import orc.ast.porc
import orc.ast.ASTWithIndex
import orc.error.compiletime.CompileLogger

import orc.util.{ ExecutionLogOutputStream, CsvWriter }
import java.io.OutputStreamWriter
import orc.ast.porc.CallContinuation
import orc.ast.porc.MethodDirect
import orc.ast.porc.TryFinally

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
        orctimizer.named.PositionChecker(res.toZipper(), co)
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

      val nodeTypesToOutput = Seq(
        classOf[Branch], classOf[Parallel], classOf[Otherwise], classOf[Trim], classOf[Future],
        classOf[Routine], classOf[Service], classOf[IfLenientMethod], classOf[Call],
        classOf[Force], classOf[GetField], classOf[Resolve], classOf[GetMethod],
        classOf[New], classOf[FieldFuture], classOf[FieldArgument])
      val optimizationsToOutput = optimizer.allOpts.map(_.name)

      val statisticsOutputs = ExecutionLogOutputStream("orctimizer-statistics", "csv", "Orctimizer static optimization statistics") map { out =>
        val traceCsv = new OutputStreamWriter(out, "UTF-8")
        (new CsvWriter(traceCsv.append(_)), traceCsv)
      }
      
      val statisticsOut = statisticsOutputs map { _._1 }

      statisticsOut foreach {
        _.writeHeader(
          Seq("Pass Number [pass]", "Maximum Number of Passes [maxPasses]") ++
            nodeTypesToOutput.map(c => s"Nodes of Type ${c.getSimpleName} before Pass [${c.getSimpleName}]") ++
            optimizationsToOutput.map(n => s"# of Applications of $n in Pass [${n.replaceAll(raw"-", raw"_")}]"))
      }

      def opt(prog: Expression, pass: Int): Expression = {
        lazy val typeCounts: collection.Map[Class[_], Int] = {
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

        optimizer.resetOptimizationCounts()
        val prog1 = optimizer(prog.toZipper(), cache)

        def optimizationCountsStr = optimizer.optimizationCounts.map(p => s"${p._1}=${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Orctimizer after pass $pass/$maxPasses: $optimizationCountsStr")
        
        statisticsOut foreach {
          _.writeRow(
            Seq(pass, maxPasses) ++
              nodeTypesToOutput.map(c => typeCounts.getOrElse(c, 0)) ++
              optimizationsToOutput.map(n => optimizer.optimizationCounts.getOrElse(n, 0)))
        }
        statisticsOutputs foreach { _._2.flush() }

        orctimizer.named.VariableChecker(prog1.toZipper(), co)
        orctimizer.named.PositionChecker(prog1.toZipper(), co)

        if (prog1 == prog || pass >= maxPasses)
          prog1
        else {
          opt(prog1, pass + 1)
        }
      }

      val e = if (co.options.optimizationFlags("orct").asBool() && pred(co))
        opt(ast, 1)
      else
        ast
        
      statisticsOutputs foreach { _._2.close() }

      e
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
        porc.PositionChecker(res.toZipper(), co)
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

      val nodeTypesToOutput = Seq(
        classOf[CallContinuation], classOf[Continuation],
        classOf[MethodCPS], classOf[MethodDirect], classOf[MethodCPSCall], classOf[MethodDirectCall], classOf[IfLenientMethod],
        classOf[Force], classOf[GetField], classOf[Resolve], classOf[GetMethod],
        classOf[New], classOf[NewFuture], classOf[Bind], classOf[BindStop], classOf[Graft],
        classOf[Spawn], classOf[NewTerminator], classOf[Kill], classOf[CheckKilled], 
        classOf[NewSimpleCounter], classOf[NewServiceCounter], classOf[NewTerminatorCounter],
        classOf[NewToken], classOf[HaltToken], classOf[SetDiscorporate], 
        classOf[TryOnException], classOf[TryFinally], 
        )
      val optimizationsToOutput = optimizer.allOpts.map(_.name)

      val statisticsOutputs = ExecutionLogOutputStream("porc-optimizer-statistics", "csv", "Porc optimizer static optimization statistics") map { out =>
        val traceCsv = new OutputStreamWriter(out, "UTF-8")
        (new CsvWriter(traceCsv.append(_)), traceCsv)
      }
      
      val statisticsOut = statisticsOutputs map { _._1 }

      statisticsOut foreach {
        _.writeHeader(
          Seq("Pass Number [pass]", "Maximum Number of Passes [maxPasses]") ++
            nodeTypesToOutput.map(c => s"Nodes of Type ${c.getSimpleName} before Pass [${c.getSimpleName}]") ++
            optimizationsToOutput.map(n => s"# of Applications of $n in Pass [${n.replaceAll(raw"-", raw"_")}]"))
      }
      
      def opt(prog: MethodCPS, pass: Int): MethodCPS = {
        lazy val typeCounts: collection.Map[Class[_], Int] = {
          val counts = new collection.mutable.HashMap[Class[_], Int]()
          def add(c: Class[_]) = {
            counts += c -> (counts.getOrElse(c, 0) + 1)
          }
          def process(n: PorcAST): Unit = {
            add(n.getClass())
            n.subtrees foreach process
          }
          process(prog)
          counts
        }
        
        optimizer.resetOptimizationCounts()
        val prog1 = optimizer(prog, analyzer).asInstanceOf[MethodCPS]

        def optimizationCountsStr = optimizer.optimizationCounts.map(p => s"${p._1} = ${p._2}").mkString(", ")
        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, s"Porc optimization pass $pass/$maxPasses: $optimizationCountsStr")
                
        statisticsOut foreach {
          _.writeRow(
            Seq(pass, maxPasses) ++
              nodeTypesToOutput.map(c => typeCounts.getOrElse(c, 0)) ++
              optimizationsToOutput.map(n => optimizer.optimizationCounts.getOrElse(n, 0)))
        }
        statisticsOutputs foreach { _._2.flush() }

        porc.VariableChecker(prog1.toZipper(), co)
        porc.PositionChecker(prog1.toZipper(), co)

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

      statisticsOutputs foreach { _._2.close() }

      e
    }
  }

  val indexPorc = new CompilerPhase[CompilerOptions, porc.MethodCPS, porc.MethodCPS] {
    import orc.ast.porc._
    val phaseName = "porc-index"
    override def apply(co: CompilerOptions) = { ast =>
      IndexAST(ast)

      ExecutionLogOutputStream("porc-ast-indicies", "csv", "Porc AST index dump") foreach { out =>
        val traceCsv = new OutputStreamWriter(out, "UTF-8")
        val statisticsOut = new CsvWriter(traceCsv.append(_))
        
        statisticsOut.writeHeader(Seq("AST Index [i]", "Source Position [position]", "Porc AST truncated [porc]"))
            
        def process(ast: PorcAST): Unit = {
          ast match {
            case a: ASTWithIndex if a.optionalIndex.isDefined =>
              val i = a.optionalIndex.get
              import orc.util.StringExtension._
              statisticsOut.writeRow((i, a.sourceTextRange.map(_.toString).getOrElse(""), a.toString.replace("\n", "").truncateTo(100)))
            case _ =>
              ()
          }
          ast.subtrees.foreach(process)
        }
        
        process(ast)
        
        out.close()
      }
      
      
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
      outputIR(1, "ext") >>>
      translate.timePhase >>>
      vClockTrans.timePhase >>>
      noUnboundVars.timePhase >>>
      fractionDefs.timePhase >>>
      typeCheck.timePhase >>>
      noUnguardedRecursion.timePhase >>>
      outputIR(2, "oil-complete") >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      outputIR(3, "oil-pruned") >>>
      abortOnError >>>
      toOrctimizer.timePhase >>>
      outputIR(4, "orct") >>>
      optimize().timePhase >>>
      outputIR(5, "orct-opt") >>>
      toPorc.timePhase >>>
      clearCachePhase >>>
      outputIR(8, "porc") >>>
      optimizePorc.timePhase >>>
      indexPorc.timePhase >>>
      outputIR(9, "porc-opt")
}
