//
// Rabin.scala -- Scala class and object Rabin
// Project OrcTests
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.dedup

import scala.util.Random

class Rabin() {
  import Rabin._
  
  private def reduce(x: Int): Int = {
    var xx = x
    for (i <- 0 until 32) {
      xx <<= 1
      if(xx >>> 31 != 0) {
        xx ^= poly
      }
    }
    xx
  }
  
  private def windowReduce(x: Int): Int = {
    var windowValue = x ^ reduceTable(0)
    for (i <- 1 until windowSize) {
      windowValue = (windowValue << 8) ^ reduceTable(windowValue >>> 24)
    }
    windowValue
  }
  
  val reduceTable = Array.tabulate(256)(reduce(_))
  val windowReduceTable = Array.tabulate(256)(windowReduce(_))
  
  def segment(data: Chunk, start: Int): Int = segment(data.buffer, data.start + start, data.end) - data.start
  
  def segment(data: Array[Byte], start: Int, end: Int): Int = {
    if (end - start < windowSize)
      return end
    
    var hash = 0
    
    @inline
    def addByte(b: Byte): Unit = {
      val x = hash >>> 24
      hash = ((hash << 8) | b) ^ reduceTable(x)
    }
    
    // Add initial items to the hash
    for (i <- start until (start + windowSize)) {
      addByte(data(i))
    }
    // Iterate over the rest of the data stepping the hash
    for (i <- (start + windowSize) until end) {
      // If we found a boundary bailout
      if ((hash & segmentMask) == 0) {
        //println(s"Found segment at $i: ${hash.formatted("%x")} ${data.slice(i - windowSize, i).toSeq}")
        return i
      }
      
      // Remove old byte
      hash ^= windowReduceTable(data(i - windowSize).toInt & 0xff)
      
      // Add new byte 
      addByte(data(i))
    }
    
    return end
  }
}

object Rabin {
  val segmentMask: Int = 0xfff
  val windowSize: Int = 64
  val poly: Int = 0x45c2b6a1
  
  /** Test entry point for Rabin
    *  
    * This should find 5 copies of WrappedArray(113, 126, -112, 1, 75, 35, 103, -127, -105, 54, ...) in the generated random data.
    */
  def main(args: Array[String]): Unit = {
    Random.setSeed(42)
    val data = Array.ofDim[Byte](1000000)
    val dataBlock = Array.ofDim[Byte](10000)
    Random.nextBytes(dataBlock)
    if (false) {
      for (i <- 0 until data.length by dataBlock.length) {
        System.arraycopy(dataBlock, 0, data, i, dataBlock.length)
      }
    } else {
      Random.nextBytes(data)
      for (_ <- 0 until 5) {
        System.arraycopy(dataBlock, 0, data, Random.nextInt(data.length - dataBlock.length), dataBlock.length)
      }
    }
    
    val rabin = new Rabin
    var a = rabin.segment(data, 0, data.length)
    while (a < data.length) {
      a = rabin.segment(data, a, data.length)
    }
  }  
}
