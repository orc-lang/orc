package orc.compile.orctimizer

import orc.compile._
import orc.ast.orctimizer
import orc.ast.porc
import orc.error.compiletime.CompileLogger
import orc.compile.tojava.OrctimizerToJava
import orc.compile.tojava.PorcToJava

/** StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces
  * and specifies that compilation will finish with named.
  *
  * @author jthywiss
  */
class OrctimizerOrcCompiler() extends PhasedOrcCompiler[String]
  with StandardOrcCompilerEnvInterface[String]
  with CoreOrcCompilerPhases {
  val toOrctimizer = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orctimizer.named.Expression] {
    val phaseName = "translate"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OILToOrctimizer()
        translator(ast)(Map())
      }
  }

  val toJava = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, String] {
    val phaseName = "toJava"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OrctimizerToJava()
        translator(ast)
      }
  }

  lazy val optimize = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, orctimizer.named.Expression] {
    import orctimizer.named._
    val phaseName = "optimize"
    override def apply(co: CompilerOptions) = { ast =>
      //println(co.options.optimizationFlags)
      val maxPasses = co.options.optimizationFlags("orct:max-passes").asInt(8)
      
      def opt(prog : Expression, pass : Int) : Expression = {
        def logAnalysis() = {
          val stats = Map(
            "limits" -> Analysis.count(prog, {
              case Limit(_) => true
              case _ => false
            }),
            "futures" -> Analysis.count(prog, {
              case Future(_) => true
              case _ => false
            }),
            "forces" -> Analysis.count(prog, {
              case Force(_) => true
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
            "concats" -> Analysis.count(prog, {
              case f Concat g => true
              case _ => false
            }),
            "nodes" -> Analysis.count(prog, (_ => true)),
            "cost" -> Analysis.cost(prog))
          val s = stats.map(p => s"${p._1} = ${p._2}").mkString(", ")
          s"Orctimizer Pass $pass/$maxPasses: $s"
        }

        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, logAnalysis())
        
        val analyzer = new ExpressionAnalyzer
        val prog1 = StandardOptimizer(co)(prog, analyzer)

        if((prog1 == prog && pass > 1) || pass > maxPasses)
          prog1
        else {
          opt(prog1, pass+1)
        }
      }
      
      val e = if(co.options.optimizationFlags("orct").asBool())
        opt(ast, 1)
      else
        ast
        
      TransformContext.clear()

      e
    }
  }
  lazy val unroll = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, orctimizer.named.Expression] {
    import orctimizer.named._
    val phaseName = "unroll"
    override def apply(co: CompilerOptions) = { ast =>
      //println(co.options.optimizationFlags)
      val maxPasses = co.options.optimizationFlags("orct:unroll-repeats").asInt(2)
      
      def opt(prog : Expression, pass : Int) : Expression = {
        val analyzer = new ExpressionAnalyzer
        val prog1 = UnrollOptimizer(co)(prog, analyzer)

        if((prog1 == prog && pass > 1) || pass > maxPasses)
          prog1
        else {
          opt(prog1, pass+1)
        }
      }
      
      val e = if(co.options.optimizationFlags("orct").asBool() &&
          co.options.optimizationFlags("orct:unroll-def").asBool())
        opt(ast, 1)
      else
        ast
        
      TransformContext.clear()

      e
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
      outputIR(1) >>>
      splitPrune.timePhase >>>
      noUnguardedRecursion.timePhase >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      outputIR(2) >>>
      toOrctimizer >>>
      outputIR(3) >>>
      optimize >>>
      outputIR(4) >>>
      unroll >>>
      outputIR(5) >>>
      optimize >>>
      outputIR(6) >>>
      toJava >>>
      outputIR(7)
}

class PorcOrcCompiler() extends OrctimizerOrcCompiler {
  val toPorc = new CompilerPhase[CompilerOptions, orctimizer.named.Expression, porc.SiteDefCPS] {
    val phaseName = "translate"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OrctimizerToPorc()
        translator(ast)
      }
  }

  val porcToJava = new CompilerPhase[CompilerOptions, porc.SiteDefCPS, String] {
    val phaseName = "toJava"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new PorcToJava()
        translator(ast)
      }
  }
  
  val optimizePorc = new CompilerPhase[CompilerOptions, porc.SiteDefCPS, porc.SiteDefCPS] {
    import orc.ast.porc._
    val phaseName = "optimize"
    override def apply(co: CompilerOptions) = { ast =>
      val maxPasses = co.options.optimizationFlags("porc:max-passes").asInt(5)
   
      def opt(prog : SiteDefCPS, pass : Int) : SiteDefCPS = {
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
        
        val prog1 = Optimizer(co)(prog, analyzer).asInstanceOf[SiteDefCPS]
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
      
      TransformContext.clear()
      e.assignNumbers()
      e
    }
  }
  override val phases =
    parse.timePhase >>>
      translate.timePhase >>>
      vClockTrans.timePhase >>>
      noUnboundVars.timePhase >>>
      fractionDefs.timePhase >>>
      typeCheck.timePhase >>>
      outputIR(1) >>>
      splitPrune.timePhase >>>
      noUnguardedRecursion.timePhase >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      outputIR(2) >>>
      toOrctimizer >>>
      outputIR(3) >>>
      optimize >>>
      outputIR(4) >>>
      unroll >>>
      outputIR(5) >>>
      optimize >>>
      outputIR(6) >>>
      toPorc >>>
      outputIR(7) >>>
      optimizePorc >>>
      outputIR(7) >>>
      porcToJava >>>
      outputIR(8)
}
