//
// wrappers.scala -- Scala objects OrcMain, FollowerRuntimeToken, and FollowerRuntimePorcE
// Project OrcTests
//
// Created by jthywiss on Sep 28, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.distrib

import orc.test.util.TestEnvironmentDescription

object OrcMain {
  def main(args: Array[String]): Unit = {
    TestEnvironmentDescription.dumpAtShutdown()
    orc.Main.main(args)
  }
}

object FollowerRuntimeToken {
  def main(args: Array[String]): Unit = {
    TestEnvironmentDescription.dumpAtShutdown()
    orc.run.distrib.token.FollowerRuntime.main(args)
  }
}

object FollowerRuntimePorcE {
  def main(args: Array[String]): Unit = {
    TestEnvironmentDescription.dumpAtShutdown()
    orc.run.distrib.porce.FollowerRuntime.main(args)
  }
}
