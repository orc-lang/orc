//
// OrctimizerAnalysisTest.scala -- OrctimizerAnalysisTest class to test orc.compiler.orctimizer.Analysis
// Project OrcTest
//
// Created by amp on Sept 14, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.data.compiler.orcimizer

import orc.ast.orctimizer.named.{ Call, Constant, Stop }

import org.junit.Test

/** @author amp
  */
class OrctimizerAnalysisTest {
  lazy val unanalyzableCall = Call(Constant(new orc.lib.net.BingSearchFactoryUsernameKey), List(), None)

  @Test
  def analyzeStop(): Unit = {
    val f = Stop()
  }
}
