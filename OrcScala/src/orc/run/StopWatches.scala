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

import orc.util.SummingStopWatch
import orc.util.DumperRegistry

object StopWatches {
  @inline
  final val workerEnabled = false
  
  /** Total time a worker uses including stealing
    */
  val workerTime = SummingStopWatch.maybe(workerEnabled)

  /** Time a worker spends blocking for work
    *
    *  Inside workerStealingTime
    */
  val workerWaitingTime = SummingStopWatch.maybe(workerEnabled)

  /** Time a worker spends executing real work
    *
    *  Inside workerTime
    */
  val workerWorkingTime = SummingStopWatch.maybe(workerEnabled)

  /** Time a worker spends executing real work
    *
    *  Inside workerWorkingTime
    */
  val workerSchedulingTime = SummingStopWatch.maybe(workerEnabled)

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
  

  if (SummingStopWatch.enabled && workerEnabled) {
    DumperRegistry.registerClear(getAndResetWorkerTimes _)
    DumperRegistry.registerCSVLineDumper(s"worker-times", "csv", "Worker time output file",
        Seq(
          "Dump ID [id]",
          "Worker task selection overhead [workerOverhead]",
          "Scheduling overhead [schedulingOverhead]",
          "Actual working time [workTime]",
          "Number of tasks [nTasks]",
          )) { name => 
      val (a, b, c, d) = getAndResetWorkerTimes()
      (name, a, b, c, d)
    }
  }

  /* Regions of interest:
   * 
   * Call dispatch
   * Java-level dispatch
   * Site implementations
   */

  @inline
  final val callsEnabled = false
  
  /** Total time spent performing calls
    */
  val callTime = SummingStopWatch.maybe(callsEnabled)
  
  /** Time spend performing Java calls after transfer out of Orc
    *
    *  Inside callTime
    */
  val javaCallTime = SummingStopWatch.maybe(callsEnabled)
  
  @inline 
  def javaCall[T](f: => T): T = {
    if (callsEnabled) {
      val s = javaCallTime.start()
      try {
        f: @inline
      } finally {
        javaCallTime.stop(s)
      }
    } else {
      f
    }
  }

  /** Time spent performing actual java execution
    *
    *  Inside javaCallTime
    */
  val javaImplTime = SummingStopWatch.maybe(callsEnabled)
  
  @inline 
  def javaImplementation[T](f: => T): T = {
    if (callsEnabled) {
      val s = javaImplTime.start()
      try {
        f: @inline
      } finally {
        javaImplTime.stop(s)
      }
    } else {
      f
    }
  }

  
  /** Time spent performing actual external work
    *
    *  Inside callTime
    */
  val implementationTime = SummingStopWatch.maybe(callsEnabled)
  
  @inline 
  def implementation[T](f: => T): T = {
    if (callsEnabled) {
      val s = implementationTime.start()
      try {
        f: @inline
      } finally {
        implementationTime.stop(s)
      }
    } else {
      f
    }
  }
  
  /** Get and reset the measured times.
    *
    *  Returns (Orc call overhead, Java dispatch overhead, time in calls, number of calls, number of Java calls)
    */
  def getAndResetCallTimes(): (Long, Long, Long, Long, Long) = {
    val (callT, nCalls) = callTime.getAndReset()
    val (javaCallT, nJavaCalls) = javaCallTime.getAndReset()
    val (javaImplT, nJavaImpl) = javaImplTime.getAndReset()
    val (implT, nImpl) = implementationTime.getAndReset()
    
    if ((nCalls - (nImpl + nJavaImpl)).abs > 5 || nJavaCalls != nJavaImpl) {
      Logger.warning(s"The number of calls and implementations are mismatched. $nCalls != ${nImpl + nJavaImpl} || $nJavaCalls != $nJavaImpl")
    }
    
    (callT - javaCallT - implT,
     javaCallT - javaImplT,
     javaImplT + implT,
     nCalls,
     nJavaCalls)
  }
  

  if (SummingStopWatch.enabled && callsEnabled) {
    DumperRegistry.registerClear(getAndResetCallTimes _)
    
    DumperRegistry.registerCSVLineDumper("call-times", "csv", "Worker time output file",
        Seq(
          "Dump ID [id]",
          "Orc call overhead [orcOverhead]",
          "Java dispatch overhead [javaOverhead]",
          "time in calls [inCalls]",
          "number of calls [nCalls]", 
          "number of Java calls [nJavaCalls]"
          )) { name => 
      val (a, b, c, d, e) = getAndResetCallTimes()
      (name, a, b, c, d, e)
    }
  }
}