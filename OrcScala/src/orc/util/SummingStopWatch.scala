//
// SummingStopWatch.scala -- Scala class SummingStopWatch
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

package orc.util

import java.util.concurrent.atomic.LongAdder

abstract class SummingStopWatch {
  def start(): Long  
  def stop(start: Long): Unit
  
  def get(): (Long, Long)
  def getAndReset(): (Long, Long)
}

object SummingStopWatch {
  @inline
  val enabled = true
  
  def apply() = {
    if (enabled) {
      new Impl
    } else {
      new Disabled
    }
  }
  
  def maybe(b: Boolean) = {
    if (enabled && b) {
      new Impl
    } else {
      new Disabled
    }
  }
  
  final class Disabled extends SummingStopWatch {
    def start(): Long = 0
    def stop(start: Long): Unit = ()
    
    def get(): (Long, Long) = (-1, -1)
    def getAndReset(): (Long, Long) = (-1, -1)
  }
  
  final class Impl extends SummingStopWatch {
    private var sum = new LongAdder()
    private var count = new LongAdder()
  
    @inline
    def start(): Long = {
      System.nanoTime()
    }
    
    @inline
    def stop(start: Long): Unit = {
      val end = System.nanoTime()
      sum.add(end - start)
      count.add(1)
    }
    
    def get() = (sum.longValue(), count.longValue())
    
    def getAndReset() = {
      val olds = sum
      val oldc = count
      sum = new LongAdder()
      count = new LongAdder()
      (olds.longValue(), oldc.longValue())
    }
  }
}
