//
// LCSMyers86.scala -- Scala trait LCS and object LCSMyers86
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.progswap

/** Compute longest common subsequence (LCS) of given sequences.
  *
  * @author jthywiss
  */
trait LCS {
  /** Compute longest common subsequence (LCS) of the given two sequences.
    *
    * Returns a 2-tuple with the identified subsequences from the two given sequences.
    */
  def lcs[A, B](seq1: Seq[A], seq2: Seq[B], isEqual: (A, B) => Boolean): (Seq[A], Seq[B])

  /** Compute longest common subsequence (LCS) of the given two sequences.
    *
    * Returns a 2-tuple with the identified subsequences from the two given sequences.
    *
    * Equivalent to lcs(a, b, e), where e calls Scala's semantic equality (==) method
    * on the left (a) sequence's elements.
    */
  def lcs[A, B](seq1: Seq[A], seq2: Seq[B]): (Seq[A], Seq[B]) = lcs(seq1, seq2, { (a: A, b: B) => a == b })
}

/** Compute longest common subsequence (LCS) of given sequences.
  *
  * Implemented using the basic algorithm from:
  *
  * Myers, Eugene W. 1986. An O(ND) Difference Algorithm and Its Variations.
  * Algorithmica 1, 2 (Nov 1986), 251-266. DOI: 10.1007/BF01840446.
  *
  * @author jthywiss
  */
object LCSMyers86 extends LCS {
  /** Compute longest common subsequence (LCS) of the given two sequences.
    *
    * Returns a 2-tuple with the identified subsequences from the two given sequences.
    */
  override def lcs[A, B](seq1: Seq[A], seq2: Seq[B], isEqual: (A, B) => Boolean): (Seq[A], Seq[B]) = {
    val len1 = seq1.size
    val len2 = seq2.size
    val sumLength = len1 + len2
    if (sumLength == 0) return (seq1.take(0), seq2.take(0))

    val endPoint = Array.ofDim[Int](sumLength + 1, sumLength * 2 + 1)
    endPoint(0)(1) = 0
    for (dPathLen <- 0 to sumLength) {
      def endPointD(i: Int) = endPoint(dPathLen)(i + sumLength)
      def endPointD_=(i: Int, x: Int) = endPoint(dPathLen)(i + sumLength) = x
      for (diagNum <- -dPathLen to dPathLen by 2) {
        var pos1 = 0
        if (diagNum == -dPathLen || diagNum != dPathLen && endPointD(diagNum - 1) < endPointD(diagNum + 1)) {
          // Step "down" (seq2) one position from farthest reaching path in diagonal k + 1
          pos1 = endPointD(diagNum + 1)
        } else {
          // Step "right" (seq1) one position from farthest reaching path in diagonal k - 1
          pos1 = endPointD(diagNum - 1) + 1
        }
        var pos2 = pos1 - diagNum
        while (pos1 < len1 && pos2 < len2 && pos1 >= 0 && pos2 >= 0 && isEqual(seq1(pos1), seq2(pos2))) {
          // Iterate across common substring (the "snake") along the diagonal
          pos1 += 1
          pos2 += 1
        }
        endPointD_=(diagNum, pos1)
        if (pos1 >= len1 && pos2 >= len2) {
          // Reached end of both sequences
          // LCS length = dPathLen
          return backtrack(seq1, seq2, isEqual, endPoint, dPathLen, diagNum)
        }
      }
      if (dPathLen < sumLength) {
        endPoint(dPathLen).copyToArray(endPoint(dPathLen + 1))
      }
    }
    /* LCS should have been found by now. See Myers (1986, p. 256). */
    throw new java.lang.AssertionError("LCS: Fell out of bottom of loop")
    //(seq1.take(0), seq2.take(0))
  }

  protected def backtrack[A, B](seq1: Seq[A], seq2: Seq[B], isEqual: (A, B) => Boolean, endPoint: Array[Array[Int]], dPathLen: Int, diagNum: Int): (Seq[A], Seq[B]) = {
    if (dPathLen == -1) return (seq1.take(0), seq2.take(0))

    val endPointOffset = (endPoint(0).size - 1) / 2
    def endPointD(i: Int) = endPoint(dPathLen)(i + endPointOffset)
    val pos1end = endPointD(diagNum)
    val pos2end = endPointD(diagNum) - diagNum
    val down = (diagNum == -dPathLen || diagNum != dPathLen && endPointD(diagNum - 1) < endPointD(diagNum + 1))
    val newDiagNum = if (down) diagNum + 1 else diagNum - 1
    val pos1start = if (down) endPointD(newDiagNum) else endPointD(newDiagNum) + 1
    val pos2start = (if (down) endPointD(newDiagNum) else endPointD(newDiagNum) + 1) - diagNum

    val pair = backtrack(seq1, seq2, isEqual, endPoint, dPathLen - 1, newDiagNum)
    (pair._1 ++ seq1.slice(pos1start, pos1end), pair._2 ++ seq2.slice(pos2start, pos2end))
  }

  def main(args: Array[String]) {
    def test(a: String, b: String) = {
      print(a + ", " + b + ": ")
      val r = lcs(a.toArray.toSeq, b.toArray.toSeq)
      println((r._1.mkString, r._2.mkString))
    }

    test("ABC", "ABC") // "ABC"
    test("XXX", "YYY") // ""
    test("XYZ", "AYZ") // "YZ"
    test("XYZ", "XBZ") // "XZ"
    test("XYZ", "XYC") // "XY"
    test("BANANA", "ATANA") // "AANA"
    test("ABCDEFG", "BCDGK") // "BCDG"
    test("AGCAT", "GAC") // "AC" or "GC" or "GA"
    test("XMJYAUZ", "MZJAWXU") // "MJAU"
  }
}
