//
// StopWatches.scala -- Scala object StopWatches
// Project OrcScala
//
// Created by amp on Jan 20, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run

import java.io.OutputStreamWriter

import orc.util.SummingStopWatch
import orc.util.DumperRegistry
import orc.util.ExecutionLogOutputStream
import orc.util.CsvWriter

object StopWatches {
  /** Total time a worker uses including stealing
    */
  val workerTime = SummingStopWatch()

  /** Time a worker spends blocking for work
    *
    *  Inside workerStealingTime
    */
  val workerWaitingTime = SummingStopWatch()

  /** Time a worker spends executing real work
    *
    *  Inside workerTime
    */
  val workerWorkingTime = SummingStopWatch()

  /** Time a worker spends executing real work
    *
    *  Inside workerWorkingTime
    */
  val workerSchedulingTime = SummingStopWatch()

  /** Get and reset the measured times.
    *
    *  Returns (Worker task selection overhead, Scheduling overhead, Actual working time)
    */
  def getAndResetWorkerTimes(): (Long, Long, Long, Long) = {
    val (workerT, _) = workerTime.getAndReset()
    val (workerWaitingT, _) = workerWaitingTime.getAndReset()
    val (workerWorkingT, nTasks) = workerWorkingTime.getAndReset()
    val (workerSchedulingT, _) = workerSchedulingTime.getAndReset()

    (workerT - workerWaitingT - workerWorkingT,
      workerSchedulingT,
      workerWorkingT - workerSchedulingT,
      nTasks)
  }
  

  if (SummingStopWatch.enabled) {
    ExecutionLogOutputStream.createOutputDirectoryIfNeeded()
    val csvOut = ExecutionLogOutputStream(s"worker-times", "csv", "Worker time output file")
    if (csvOut.isDefined) {
      val traceCsv = new OutputStreamWriter(csvOut.get, "UTF-8")
      val csvWriter = new CsvWriter(traceCsv.append(_))
  
      val tableColumnTitles = Seq(
          "Dump ID [id]",
          "Worker task selection overhead [workerOverhead]",
          "Scheduling overhead [schedulingOverhead]",
          "Actual working time [workTime]",
          "Number of tasks [nTasks]",
          )
      csvWriter.writeHeader(tableColumnTitles)
  
      DumperRegistry.register { name => 
        val (a, b, c, d) = getAndResetWorkerTimes()
        csvWriter.writeRow((name, a, b, c, d))
        traceCsv.flush()
      }
    }
  }

  // TODO: This could be extended to replace the RuntimeProfiler code, probably with lower overhead and faster processing.
}