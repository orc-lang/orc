package orc.compile.orctimizer

import orc.compile._

/** StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces
  * and specifies that compilation will finish with named.
  *
  * @author jthywiss
  */
class OrctimizerOrcCompiler() extends PhasedOrcCompiler[orc.ast.orctimizer.named.Expression]
  with StandardOrcCompilerEnvInterface[orc.ast.orctimizer.named.Expression]
  with CoreOrcCompilerPhases {
  val toOrctimizer = new CompilerPhase[CompilerOptions, orc.ast.oil.named.Expression, orc.ast.orctimizer.named.Expression] {
    val phaseName = "translate"
    override def apply(co: CompilerOptions) =
      { ast =>
        val translator = new OILToOrctimizer()
        translator(ast)
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
      noUnguardedRecursion.timePhase >>>
      outputIR(1) >>>
      splitPrune.timePhase >>>
      removeUnusedDefs.timePhase >>>
      removeUnusedTypes.timePhase >>>
      outputIR(2) >>>
      toOrctimizer >>>
      outputIR(2)
}
