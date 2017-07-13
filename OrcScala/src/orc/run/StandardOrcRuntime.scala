//
// StandardOrcRuntime.scala -- Scala class StandardOrcRuntime
// Project OrcScala
//
// Created by dkitchin on Jun 24, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run

import orc.OrcRuntime
import orc.OrcOptions
import orc.run.extensions._

class StandardOrcRuntime(engineInstanceName: String) extends Orc(engineInstanceName)
  with StandardInvocationBehavior
  with OrcWithWorkStealingScheduler
  //with OrcWithThreadPoolScheduler
  with SupportForRwait
  with SupportForSynchronousExecution
  with SwappableASTs
