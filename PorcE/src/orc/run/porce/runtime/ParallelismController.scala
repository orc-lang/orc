//
// ParallelismController.scala -- Scala class ParallelismController
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime

import scala.collection.JavaConverters._
import scala.collection.mutable

import orc.run.porce.{ Logger, ParallelismNode, PorcERootNode, SpecializationConfiguration }

import com.oracle.truffle.api.CompilerAsserts
import com.oracle.truffle.api.nodes.NodeUtil

class ParallelismController(execution: PorcEExecution) {
  //val rootsToUpdate = mutable.Buffer[PorcERootNode]()

  def enqueue(root: PorcERootNode): Unit = synchronized {
    CompilerAsserts.neverPartOfCompilation("ParallelismController")

    // If there is no parallelism choice, just turn off profiling immediately.
    if (!NodeUtil.findAllNodeInstances(root, classOf[ParallelismNode]).asScala.exists(_.isParallelismChoiceNode)) {
      root.setProfiling(false)
      Logger.info(s"Ignored: ${root}")
      return
    }

    //rootsToUpdate += root
    val rootsToUpdate = (Seq() ++ execution.allPorcERootNodes).filter(r => r.getTotalCalls > 0)

    // Check the profiling data and potentially perform control
    val parallelismNodes = rootsToUpdate.flatMap(r => {
      val ps = NodeUtil.findAllNodeInstances(r, classOf[ParallelismNode]).asScala
      ps
    }).filter(_.isParallelismChoiceNode)
    val totalExecutionCount = parallelismNodes.map(_.getExecutionCount).sum

    Logger.info(s"Enqueued: ${root} (totalExecutionCount = $totalExecutionCount)")

    if (totalExecutionCount > SpecializationConfiguration.MinimumExecutionCountForParallelismController) {
      val sortedNodes = parallelismNodes.sortBy(_.getExecutionCount)
      val targetCount = totalExecutionCount / 100
      val prefixLen = sortedNodes.scanLeft(0L)(_ + _.getExecutionCount).indexWhere(_ > targetCount)

      // Disable profiling in roots we are working with.
      for (r <- rootsToUpdate) {
        r.setProfiling(false)
      }

      // Set the initial prefixLen nodes to be parallel and all others not to be
      for ((n, i) <- sortedNodes.zipWithIndex) {
        n.setParallel(i < prefixLen)
      }

      Logger.info(s"Finished profiling: n roots = ${rootsToUpdate.size}, n parallel = $prefixLen, n nodes = ${sortedNodes.size}")

      // Clear roots for next pass.
      //rootsToUpdate.clear()
    }
  }
}
