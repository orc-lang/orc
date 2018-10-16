//
// DistributedOrcCompiler.scala -- Scala class DistributedOrcCompiler
// Project OrcScala
//
// Created by jthywiss on Oct 14, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.distrib

import orc.ast.oil.named
import orc.compile.{ CompilerOptions, CompilerPhase, StandardOrcCompiler }
import orc.compile.orctimizer.PorcOrcCompiler

/** DistributedOrcCompiler is the StandardOrcCompiler with a new
 *  valueSetAnalysis phase.
  *
  * @author jthywiss
  */
class DistributedOrcCompiler() extends StandardOrcCompiler() {

  val valueSetAnalysis = new CompilerPhase[CompilerOptions, named.Expression, named.Expression] {
    val phaseName = "valueSetAnalysis"
    override def apply(co: CompilerOptions) = { ast =>
      ValueSetAnalysis(ast)
    }
  }

  override val phases =
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
    valueSetAnalysis.timePhase >>>
    deBruijn.timePhase >>>
    outputOil
}

/** DistributedPorcOrcCompiler is a PorcOrcCompiler with a new
 *  valueSetAnalysis phase.
  *
  * @author jthywiss
  */
class DistributedPorcOrcCompiler() extends PorcOrcCompiler() {

  val valueSetAnalysis = new CompilerPhase[CompilerOptions, named.Expression, named.Expression] {
    val phaseName = "valueSetAnalysis"
    override def apply(co: CompilerOptions) = { ast =>
      ValueSetAnalysis(ast)
    }
  }

  override val phases =
    parse.timePhase >>>
      outputIR(1, "ext") >>>
      (loadCachedPorc.timePhase).orElse(
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
        valueSetAnalysis.timePhase >>>
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
        saveCachedPorc.timePhase
      ) >>>
      outputIR(9, "porc-opt")
}
