//
// DelimeterSeparatedTabularTextWriter.scala -- Scala classed DelimeterSeparatedTabularTextWriter, CsvWriter, TsvWriter, and WikiCreoleTableWriter
// Project OrcScala
//
// Created by jthywiss on Sep 3, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

/* Trick for union types using implicits until we are using Dotty */
protected class TraversableOrProduct[-T]
protected object TraversableOrProduct {
  implicit object IterableWitness extends TraversableOrProduct[TraversableOnce[_]]
  implicit object ProductWitness extends TraversableOrProduct[Product]
}

/** Writes data tables (2-dimensional Traversables) in a textual format
  * where rows are lines and columns are separated by delimiters.
  *
  * @author jthywiss
  */
abstract class DelimeterSeparatedTabularTextWriter(write: String => Unit) {

  def this(appendable: Appendable) = this(appendable.append(_))

  /** IANA-registered media type of this writer's output.
    * (list at http://www.iana.org/assignments/media-types/ )
    */
  val mediaType: String

  /** The string used at the start of a row. */
  val rowPrefix: String

  /** The separator between cells. */
  val delimiter: String

  /** The string used at the end of a row. */
  val lineTerminator: String

  /** A set of characters that require a cell be quoted. */
  val requiresQuotes: String

  /** The character that is used around cells that must be quoted. */
  val quoteCharacter: String

  /** A set of characters that require an escape prefix. */
  val requiresEscape: String

  /** The string that is used to escape the quote character within escaped cells. */
  val escapePrefix: String

  /** A set of characters that might be trimmed from the beginning or end of cell values. */
  val whitespace: String

  def writeTable[R: TraversableOrProduct](columnTitles: TraversableOnce[_], rows: TraversableOnce[R]): Unit = {
    writeHeader(columnTitles)
    writeRows(rows)
  }

  def writeHeader(columnTitles: TraversableOnce[_]): Unit = {
    writeRow(columnTitles)
  }

  def writeRows[R: TraversableOrProduct](rows: TraversableOnce[R]): Unit = {
    rows.foreach(row => writeRow(row))
  }

  def writeRow[R: TraversableOrProduct](rowData: R, rowPrefix: String = rowPrefix, delimiter: String = delimiter, lineTerminator: String = lineTerminator): Unit = {
    rowData match {
      case to: TraversableOnce[_] => writeRow(to, rowPrefix, delimiter, lineTerminator)
      case p: Product => writeRow(p.productIterator, rowPrefix, delimiter, lineTerminator)
    }
  }

  def writeRow(rowData: TraversableOnce[_], rowPrefix: String, delimiter: String, lineTerminator: String): Unit = {
    var isFirstCell = true
    write(rowPrefix)
    rowData.foreach(column => {
      if (isFirstCell) {
        isFirstCell = false
      } else {
        write(delimiter)
      }
      writeCell(column)
    })
    write(lineTerminator)
  }

  def writeCell(value: Any): Unit = {
    def escape(badChars: String, esc: String)(inCh: Char) =
      if (badChars.contains(inCh)) esc + inCh else inCh.toString
    val str = if (value != null) value.toString else ""
    val mustQuote =
        (str.nonEmpty && (whitespace.contains(str.head) || whitespace.contains(str.last))) ||
        (requiresQuotes.nonEmpty && requiresQuotes.exists(str.contains(_)))
    val escapedStr = if (requiresEscape.nonEmpty) str.flatMap(escape(requiresEscape, escapePrefix)(_)) else str
    if (mustQuote) {
      write(quoteCharacter)
      write(escapedStr)
      write(quoteCharacter)
    } else {
      write(escapedStr)
    }
  }
}

/** Comma-separated value writer, per RFC 4180 and https://www.w3.org/TR/tabular-data-model/
  *
  * @author jthywiss
  */
class CsvWriter(write: String => Unit) extends DelimeterSeparatedTabularTextWriter(write) {

  def this(appendable: Appendable) = this(appendable.append(_))

  /** IANA-registered media type of this writer's output.
    * (list at http://www.iana.org/assignments/media-types/ )
    */
  val mediaType: String = "text/csv"

  /** The string used at the start of a row. */
  val rowPrefix: String = ""

  /** The separator between cells. */
  val delimiter: String = ","

  /** The string used at the end of a row. */
  val lineTerminator: String = "\r\n"

  /** A set of characters that require a cell be quoted. */
  val requiresQuotes: String = "\",\r\n"

  /** The character that is used around cells that must be quoted. */
  val quoteCharacter: String = "\""

  /** A set of characters that require an escape prefix. */
  val requiresEscape: String = "\""

  /** The string that is used to escape the quote character within escaped cells. */
  val escapePrefix: String = "\""

  /** The string that is used around escaped cells. */
  val whitespace: String = " \t"
}

/** Tab-separated value writer, per http://www.iana.org/assignments/media-types/text/tab-separated-values
  *
  * @author jthywiss
  */
class TsvWriter(write: String => Unit) extends DelimeterSeparatedTabularTextWriter(write) {

  def this(appendable: Appendable) = this(appendable.append(_))

  /** IANA-registered media type of this writer's output.
    * (list at http://www.iana.org/assignments/media-types/ )
    */
  val mediaType: String = "text/tab-separated-values"

  /** The string used at the start of a row. */
  val rowPrefix: String = ""

  /** The separator between cells. */
  val delimiter: String = "\t"

  /** The string used at the end of a row. */
  val lineTerminator: String = "\r\n"

  /** A set of characters that require a cell be quoted. */
  val requiresQuotes: String = ""

  /** The character that is used around cells that must be quoted. */
  val quoteCharacter: String = ""

  /** A set of characters that require an escape prefix. */
  val requiresEscape: String = ""

  /** The string that is used to escape the quote character within escaped cells. */
  val escapePrefix: String = ""

  /** A set of characters that might be trimmed from the beginning or end of cell values. */
  val whitespace: String = " \t"
}

/** WikiCreole table writer.  See http://www.wikicreole.org/wiki/Creole1.0#section-Creole1.0-Tables
  *
  * @author jthywiss
  */
class WikiCreoleTableWriter(write: String => Unit) extends DelimeterSeparatedTabularTextWriter(write) {

  def this(appendable: Appendable) = this(appendable.append(_))

  /** IANA-registered media type of this writer's output.
    * (list at http://www.iana.org/assignments/media-types/ )
    */
  val mediaType: String = "text/X-wikicreole"

  /** The string used at the start of a row. */
  val rowPrefix: String = "|"

  /** The separator between cells. */
  val delimiter: String = "|"

  /** The string used at the end of a row. */
  val lineTerminator: String = "|\r\n"

  /** A set of characters that require a cell be quoted. */
  val requiresQuotes: String = ""

  /** The character that is used around cells that must be quoted. */
  val quoteCharacter: String = ""

  /** A set of characters that require an escape prefix. */
  val requiresEscape: String = "|"

  /** The string that is used to escape the quote character within escaped cells. */
  val escapePrefix: String = "\\"

  /** A set of characters that might be trimmed from the beginning or end of cell values. */
  val whitespace: String = " \t"

  override def writeHeader(columnTitles: TraversableOnce[_]): Unit = {
    writeRow(columnTitles, rowPrefix = "|=", delimiter = "|=")
  }
}
