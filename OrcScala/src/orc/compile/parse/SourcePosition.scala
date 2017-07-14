//
// SourcePosition.scala -- Scala class SourcePosition
// Project OrcScala
//
// Created by jthywiss on Jul 23, 2016.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.parse

import scala.util.parsing.input.Position

import orc.util.{ TextPosition, TextRange }

/** A TextPosition for Orc source files.  Also provides content of the line
  * of the source code for the position, to be used in error messages.
  */
class OrcSourcePosition(
  override val resource: OrcInputContext,
  override val offset: TextPosition[OrcInputContext]#CharacterNumber) extends TextPosition[OrcInputContext] {
  assert(resource != null, "resource must be non-null")
  assert(offset >= 0, "offset must be nonnegative")

  protected def getLineCol() = resource.lineColForCharNumber(offset)
  protected val lineCol = getLineCol()
  override val line: TextPosition[OrcInputContext]#LineNumber = lineCol._1
  override val column: TextPosition[OrcInputContext]#ColumnNumber = lineCol._2

  override def resourceDescription = resource.descr

  def lineContent: String = resource.lineText(line)

  def lineContentWithCaret = lineContent + "\n" + lineContent.take(column - 1).map { x => if (x == '\t') x else ' ' } + "^"

}

/** Trait for values that have an orcSourcePosition member. */
trait HasOrcSourcePosition {
  val orcSourcePosition: OrcSourcePosition
}

/** An OrcSourcePosition that is compatible with Scala's parser combinator
  * library. Parsers must get input from Readers, which must supply a pos of
  * type scala.util.parsing.input.Position, so we're stuck with having to
  * wrap our positions.
  */
class ScalaOrcSourcePosition(val orcSourcePosition: OrcSourcePosition) extends Position with HasOrcSourcePosition {
  override def line = orcSourcePosition.line
  override def column = orcSourcePosition.column
  override protected def lineContents = orcSourcePosition.lineContent
  override def toString = orcSourcePosition.toString()
  override def longString = orcSourcePosition.lineContentWithCaret
  override def <(that: Position) = orcSourcePosition.<(that.asInstanceOf[ScalaOrcSourcePosition].orcSourcePosition)
}

/** Extract the OrcSourcePosition from an scala.util.parsing.input.Position,
  * if possible.
  */
object ToTextPosition {
  def apply(pos: Position) = pos match {
    case orh: HasOrcSourcePosition => orh.orcSourcePosition
    case _ => throw new AssertionError("this pos is not an HasOrcSourcePosition: " + pos)
  }
}

/** A TextRange for Orc source files.  OrcSourceRange also provides content
  * of the lines of the source code for the range, to be used in error
  * messages.
  */
class OrcSourceRange(
  posns: (OrcSourcePosition, OrcSourcePosition)) extends TextRange[OrcSourcePosition, OrcInputContext] {

  override def start: OrcSourcePosition = posns._1
  override def end: OrcSourcePosition = posns._2

  def lineContent: String = {
    if (start.resource == end.resource) {
      val sb = new StringBuffer((1 + end.line - start.line) * 80)
      for (l <- start.line to end.line) {
        sb.append(start.resource.lineText(l))
        if (l != end.line) sb.append('\n')
      }
      sb.toString
    } else {
      /* Range crosses resources */
      start.resource.lineText(start.line) + '\n' +
        " . . . . . . " + '\n' +
        end.resource.lineText(end.line)
    }
  }

  def lineContentWithCaret = {
    if (start.resource == end.resource) {
      val sb = new StringBuffer((1 + end.line - start.line) * 80 * 2)
      for (l <- start.line to end.line) {
        sb.append(start.resource.lineText(l))
        sb.append('\n')
        val precedingText = if (l != start.line) "" else start.resource.lineText(l).take(start.column - 1)
        val subsequentTextLen = if (l != end.line) 0 else end.resource.lineText(l).length - (end.column - 1)
        val inRangeText = start.resource.lineText(l).drop(precedingText.length).dropRight(subsequentTextLen)
        sb.append(precedingText.map { x => if (x == '\t') x else ' ' })
        sb.append(inRangeText.map { x => if (x == '\t') x else '^' })
        if (l != end.line) sb.append('\n')
      }
      sb.toString
    } else {
      /* Range crosses resources */
      val startLine = start.resource.lineText(start.line)
      val endLine = end.resource.lineText(end.line)
      startLine + '\n' +
        startLine.take(start.column - 1).map { x => if (x == '\t') x else ' ' } +
        startLine.drop(start.column - 1).map { x => if (x == '\t') x else '^' } +
        " . . . . . . " + '\n' +
        endLine + '\n' +
        endLine.take(end.column - 1).map { x => if (x == '\t') x else '^' } +
        endLine.drop(end.column - 1).map { x => if (x == '\t') x else ' ' }
    }
  }
}

/** Trait for values that have an orcSourceRange member. */
trait HasOrcSourceRange {
  val orcSourceRange: OrcSourceRange
}

/** An OrcSourceRange that is compatible with Scala's parser combinator
  * library. Parsers must get input from Readers, which must supply a pos of
  * type scala.util.parsing.input.Position, so we're stuck with having to
  * wrap our ranges.
  */
class ScalaOrcSourceRange(val orcSourceRange: OrcSourceRange) extends Position with HasOrcSourceRange {
  override def line = orcSourceRange.start.line
  override def column = orcSourceRange.start.line
  override protected def lineContents = orcSourceRange.lineContent
  override def toString = orcSourceRange.toString()
  override def longString = orcSourceRange.lineContentWithCaret
  override def <(that: Position) = orcSourceRange.coversOrEquals(that.asInstanceOf[ScalaOrcSourceRange].orcSourceRange).getOrElse(false)
}

/** Extract the OrcSourceRange from an scala.util.parsing.input.Position, if
  * possible.
  */
object ToTextRange {
  def apply(pos: Position) = pos match {
    case orh: HasOrcSourceRange => orh.orcSourceRange
    case _ => throw new AssertionError("this pos is not an HasOrcSourceRange: " + pos)
  }
}
