package orc.compile.orctimizer

import orc.compile._
import orc.ast.orctimizer
import orc.error.compiletime.CompileLogger

/** StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces
  * and specifies that compilation will finish with named.
  *
  * @author jthywiss
  */
class OrctimizerOrcCompiler() extends PhasedOrcCompiler[orc.ast.orctimizer.named.Expression]
  with StandardOrcCompilerEnvInterface[orc.ast.orctimizer.named.Expression]
  with CoreOrcCompilerPhases {
  val toOrctimizer = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orctimizer.named.Expression] {
    val phaseName = "translate"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OILToOrctimizer()
        translator(ast)(Map())
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
            "nodes" -> Analysis.count(prog, (_ => true)),
            "cost" -> Analysis.cost(prog))
          val s = stats.map(p => s"${p._1} = ${p._2}").mkString(", ")
          s"Orctimizer Pass $pass/$maxPasses: $s"
        }

        co.compileLogger.recordMessage(CompileLogger.Severity.DEBUG, 0, logAnalysis())
        
        val analyzer = new ExpressionAnalyzer
        val prog1 = Optimizer(co)(prog, analyzer)

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
      outputIR(4)
}
