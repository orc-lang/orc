//
// StandardOrcCompiler.scala -- Scala class/trait/object StandardOrcCompiler
// Project OrcScala
//
// $Id$
//
// Created by amp on Sep 25, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile

/** StandardOrcCompiler extends CoreOrcCompiler with "standard" environment interfaces
  * and specifies that compilation will finish with named.
  *
  * @author jthywiss
  */
class StandardOrcCompiler() extends PhasedOrcCompiler[orc.ast.oil.nameless.Expression]
  with StandardOrcCompilerEnvInterface[orc.ast.oil.nameless.Expression]
  with CoreOrcCompilerPhases {
  ////////
  // Compose phases into a compiler
  ////////

  val phases =
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
      deBruijn.timePhase >>>
      outputOil
}