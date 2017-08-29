//
// SparseBinaryFraction.scala -- Scala class SparseBinaryFraction
// Project OrcScala
//
// Created by amp on Aug 26, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import scala.collection.SortedSet
import scala.collection.SortedSet

/** A non-negative binary fraction represented as a set of ints representing the positions of 1s in the binary representation.
  *
  * The value must be between 0 and 1.
  *
  * This representation only increases in size with the number of set bits, not the value of the integer.
  */
case class SparseBinaryFraction(ones: SortedSet[Int]) {
  require(ones.isEmpty || ones.firstKey >= 0)
  
  /** Subtract 1/(2**i) from this number.
    */
  def subtractBit(i: Int): SparseBinaryFraction = {
    if (i > ones.lastKey) {
      throw new IllegalArgumentException(s"1/(2^${i}) is greater than $this")
    } else {
      var k = i
      var v = this
      while (!v.ones.contains(k)) {
        v = SparseBinaryFraction(v.ones + k)
        k -= 1
      }
      SparseBinaryFraction(v.ones - k)
    }
  }

  /** Add 1/(2**i) to this number.
    */
  def addBit(i: Int): SparseBinaryFraction = {
    var k = i
    var v = this
    // TODO: PERFORMANCE: Since the set is sorted we could iterate in order looking for the value larger than i
    while (v.ones.contains(k)) {
      v = SparseBinaryFraction(v.ones - k)
      k -= 1
      if (k < 0) {
        throw new IllegalArgumentException(s"1/(2^${i}) is greater than 2-${this}")
      }
    }
    SparseBinaryFraction(v.ones + k)
  }

  /** Return a bit and a new SparseBinaryFraction which sum to this.
    */
  def split(): (Int, SparseBinaryFraction) = {
    if (ones.size == 0) {
      throw new IllegalArgumentException("this is 0")
    } else if (ones.size == 1) {
      val b = ones.lastKey + 1
      (b, SparseBinaryFraction(SortedSet(b)))      
    } else {
      val b = ones.lastKey
      (b, SparseBinaryFraction(ones - b))
    }
  }
  
  override def toString() = {
    s"SparseBinaryFraction(1/(${ones.toSeq.map(i => s"2^${i}").mkString("+")}))"
  }
}

object SparseBinaryFraction {
  /** Representation of 1.
    */
  val one = SparseBinaryFraction(SortedSet(0))

  /** Representation of 0.
    */
  val zero = SparseBinaryFraction(SortedSet[Int]())
}