//
// Range.scala -- Scala class Range
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values.sites

/** Represent a range of numbers lower bounded by a natural and upper bounded
  * by a natural or infinity.
  *
  * @author amp
  */
case class Range(mini: Int, maxi: Option[Int]) {
  assert(mini >= 0)
  assert(maxi map { _ >= mini } getOrElse true)

  /** True iff this contains only values greater than or equal to n.
    */
  def >=(n: Int) = mini >= n
  /** True iff this contains only values greater than n.
    */
  def >(n: Int) = mini > n
  /** True iff this contains only values less than or equal to n.
    */
  def <=(n: Int) = maxi map { _ <= n } getOrElse false
  /** True iff this contains only values less than n.
    */
  def <(n: Int) = maxi map { _ < n } getOrElse false

  /** True iff this contains only n.
    */
  def only(n: Int) = mini == n && maxi == Some(n)

  /** Return the intersection of two ranges or None is the intersection is empty.
    */
  def intersectOption(r: Range): Option[Range] = {
    val n = mini max r.mini
    val m = (maxi, r.maxi) match {
      case (Some(x), Some(y)) => Some(x min y)
      case (Some(_), None) => maxi
      case (None, Some(_)) => r.maxi
      case (None, None) => None
    }
    if (m map { _ >= n } getOrElse true)
      Some(Range(n, m))
    else
      None
  }

  def intersect(r: Range): Range = {
    intersectOption(r).getOrElse {
      throw new IllegalArgumentException("Ranges do not overlap and range cannot "
        + "represent the empty range. You may have mixed in an incompatible set of"
        + " site metadata traits.")
    }
  }

  /** Return the union of two ranges.
    */
  def union(r: Range): Range = {
    val n = mini min r.mini
    // If either is None m is also None
    val m = for (ma <- maxi; mb <- r.maxi) yield ma max mb
    Range(n, m)
  }

  /** Return a range containing all results of summing values from this and r.
    */
  def +(r: Range) = {
    Range(mini + r.mini, (maxi, r.maxi) match {
      case (Some(n), Some(m)) => Some(n + m)
      case _ => None
    })
  }
  /** Return a range containing all results of multiplying values from this and r.
    */
  def *(r: Range) = {
    Range(mini * r.mini, (maxi, r.maxi) match {
      case (Some(0), _) | (_, Some(0)) => Some(0)
      case (Some(n), Some(m)) => Some(n * m)
      case _ => None
    })
  }

  /**
    */
  def supersetOf(r: Range) = {
    mini <= r.mini && ((maxi, r.maxi) match {
      case (Some(n), Some(m)) => n >= m
      case (None, _) => true
      case _ => false
    })
  }

  /**
    */
  def subsetOf(r: Range) = {
    r supersetOf this
  }

  /** Return a range similar to this but that upper bounded by lim. Unlike intersection,
    * if lim is less than the lower bound of this return Range(lim, lim).
    */
  def limitTo(lim: Int) = {
    val n = mini min lim
    val m = maxi map (_ min lim) getOrElse lim
    Range(n, m)
  }

  /** Return a range which includes 0 but has the same upper bound as this.
    *
    */
  def mayHalt = {
    Range(0, maxi)
  }

  override def toString() = s"<$mini-${maxi.getOrElse("ω")}>"
}

object Range {
  def apply(n: Int, m: Int): Range = Range(n, Some(m))

  def apply(r: (Int, Option[Int])): Range = {
    Range(r._1, r._2)
  }
}
