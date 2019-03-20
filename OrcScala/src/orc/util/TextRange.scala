//
// TextRange.scala -- Scala traits TextPosition and TextRange
// Project OrcScala
//
// Created by jthywiss on Jul 20, 2016.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

/** A position in a line-oriented character string.
  *
  * [This description is based on, but modified from RFC 5147:]
  * A position does not identify an actual substring of text, but a position
  * inside the text, which can be regarded as a substring of length zero. The
  * use case for positions is to provide pointers for applications that may
  * use them to implement functionalities such as "insert some text here",
  * which needs a position rather than a substring. Positions are counted from
  * zero; position zero being before the first character or line of a line-
  * oriented character string. Thus, a character string having one character
  * has two positions, one before the first character (offset 0), and one
  * after the first character (offset 1).
  *
  * Note: Line and column numbers are one-based, following strong convention.
  * This is different from RFC 5147.
  *
  * @author jthywiss
  */
trait TextPosition[R] {
  /** For 0-based indices */
  type NonnegativeIntegralType = Int
  /** For 1-based indices */
  type PositiveIntegralType = Int

  type CharacterNumber = NonnegativeIntegralType
  type LineNumber = PositiveIntegralType
  type ColumnNumber = PositiveIntegralType

  /** Some type of reference to the container of the lines of text. */
  val resource: R
  /** Number of characters (not bytes) in the text that precede this position. Zero-based. */
  val offset: CharacterNumber
  /** Line number of the text that contains this position. One-based. */
  val line: LineNumber
  /** Column number of the text that this position immediately precedes. One-based. */
  val column: ColumnNumber
  /** A string identifying the resource, to be used when printing this position. (E.g., pathname) */
  def resourceDescription: String

  override def equals(that: Any) = that match {
    case thatTP: TextPosition[_] => {
      if (resource.equals(thatTP.resource) && offset.equals(thatTP.offset)) {
        assert(line.equals(thatTP.line) && column.equals(thatTP.column), ".equals doesn't report equal line & column")
        assert(thatTP.resource.equals(resource) && thatTP.offset.equals(offset), ".equals symmetry violation")
        true
      } else {
        false
      }
    }
  }

  override def hashCode = resource.hashCode + offset

  override def toString = resourceDescription + ':' + line + ':' + column

  def <(that: TextPosition[R]) = this.resource == that.resource && this.offset < that.offset
  def <=(that: TextPosition[R]) = this == that || this < that
  def >(that: TextPosition[R]) = this.resource == that.resource && this.offset > that.offset
  def >=(that: TextPosition[R]) = this == that || this > that

}

/** A range of positions specifying a substring of a line-oriented character
  * string.
  *
  * [This description is based on, but modified from RFC 5147:]
  * Ranges identify substring of a character string that have a length that
  * may be greater than zero. Ranges specify both a lower and an upper bound.
  * The end of a range must have a value greater than or equal to the start.
  * A range with identical start and end is legal and identifies a range of
  * length zero, which is equivalent to a position.
  *
  * @author jthywiss
  */
trait TextRange[P <: TextPosition[R], R] {
  /** TextPosition immediately preceding this range. */
  def start: P
  /** TextPosition immediately following this range. */
  def end: P

  assert(start <= end, "start must precede end")

  override def toString() = {
    if (start.resource == end.resource) {
      if (start.line == end.line) {
        if (start.column == end.column) {
          start.resourceDescription + ':' + start.line + ':' + start.column
        } else {
          start.resourceDescription + ':' + start.line + ':' + start.column + '-' + end.column
        }
      } else {
        start.resourceDescription + ':' + start.line + ':' + start.column + '-' + end.line + ':' + end.column
      }
    } else {
      start.resourceDescription + ':' + start.line + ':' + start.column + '-' + end.resourceDescription + ':' + end.line + ':' + end.column
    }
  }

  private def bothDefinedOnSameResource(that: TextRange[P, R]) =
    this.start.resource == that.start.resource && this.end.resource == that.end.resource

  private def ifBothDefinedOnSameResource[T](that: TextRange[P, R], f: () => T): Option[T] =
    if (bothDefinedOnSameResource(that)) Some(f()) else None

  /** The two ranges begin and end at exactly the same position.
    */
  def equalRange(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => this.start == that.start && this.end == that.end)

  /** Two ranges r and s abut if and only if r precedes s, yet there
    * are no characters between r and s, and r and s do not overlap.
    */
  def abuts(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => this.end == that.start)

  /** This range ends _strictly_ precedes the given range begins.
    * I.e., there are characters between them.
    */
  def precedesStrictly(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => this.end < that.start)

  /** Precedes or abuts.
    *
    * Convenience method:
    * r precedesOrAbuts s ≣ r precedes s ∨ r abuts j
    */
  def precedesOrAbuts(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => precedesStrictly(that).get || abuts(that).get)

  /** The given position falls inside (non-strictly) this range. */
  def contains(that: TextPosition[R]) =
    if (this.start.resource == that.resource && this.end.resource == that.resource) {
      this.start <= that && that <= this.end
    } else {
      None
    }

  /** The given range falls inside (non-strictly) this range. */
  def coversOrEquals(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => this.start <= that.start && this.end <= that.end)

  /** This range begins _strictly_ before and ends _strictly_ after the
    * given range.
    */
  def coversStrictly(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => this.start < that.start && this.end < that.end)

  /** This range begins after and ends before the given range; one of
    * these bounds must be strict. In other words, this range is contained
    * in the given range.
    */
  def containedStrictlyIn(that: TextRange[P, R]) =
    ifBothDefinedOnSameResource(that, () =>
      this.start >= that.start && this.end < that.end ||
        this.start > that.start && this.end <= that.end)

  /** Contained strictly in or equals.
    *
    * Convenience method:
    * r containedInOrEquals s ≣ r containedStrictlyIn s ∨ r = s
    */
  def containedInOrEquals(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => containedStrictlyIn(that).get || equalRange(that).get)

  /** The ranges begin at the same position.
    */
  def startsWith(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => this.start == that.start)

  /** The ranges end at the same position.
    */
  def endsWith(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => this.end == that.end)

  /** The ranges do not overlap in any way.
    *
    * Convenience method:
    * r disjointWith s ≣ r precedesOrAbuts s ∨ s precedesOrAbuts r
    */
  def disjointWith(that: TextRange[P, R]) = ifBothDefinedOnSameResource(that, () => this.precedesOrAbuts(that).get || that.precedesOrAbuts(this).get)

}
