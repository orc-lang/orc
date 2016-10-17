//
// Input.scala -- Scala classes, traits, and objects relating to parser input sources
// Project OrcScala
//
// Created by jthywiss on Jun 6, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.parse

import java.io.{ File, FileInputStream, IOException, InputStreamReader, StringReader }
import java.net.{ URI, URL }

import scala.collection.immutable.PagedSeq
import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.input.PagedSeqReader

import orc.util.TextPosition

/** Presents the scala.util.parsing.input.Reader interface, which is a
  * position in a particular sequence of values, namely Chars in our case.
  *
  * This interface required for the input to be parsed to Scala's parser
  * combinator library.
  *
  * @author jthywiss
  */
class OrcReader(val inputContext: OrcInputContext, seq: PagedSeq[Char], offset: Int) extends PagedSeqReader(seq, offset) {
  if (inputContext == null) throw new NullPointerException("OrcReader.<init>(inputContext == null)")
  if (seq == null) throw new NullPointerException("OrcReader.<init>(seq == null)")

  override def rest: OrcReader =
    if (seq.isDefinedAt(offset)) new OrcReader(inputContext, seq, offset + 1)
    else this

  override def drop(n: Int): OrcReader =
    new OrcReader(inputContext, seq, offset + n)

  override lazy val pos: ScalaOrcSourcePosition = new ScalaOrcSourcePosition(new OrcSourcePosition(inputContext, offset))
}

object OrcReader {
  def apply(inputContext: OrcInputContext, in: java.io.Reader): OrcReader = new OrcReader(inputContext, PagedSeq.fromReader(in), 0)
}

/** Container for a reader, its description, and a means of obtaining
  * "sub-input"s. By sub-input, we mean another <code>OrcInputContext</code>
  * referring to an input stream that corresponds to the given name.
  * This name is interpreted as a relative name in the namespace of the
  * original <code>OrcInputContext</code>.
  * <p>
  * This is intended to abstract reading from files, JAR resources,
  * network URLs, strings, etc. with relative file name references such
  * as those in <code>include</code> statements.
  *
  * @author jthywiss
  */
trait OrcInputContext {
  val reader: OrcReader
  val descr: String
  def toURI: URI
  def toURL: URL

  protected def resolve(baseURI: URI, pathElements: String*): URI = {
    def allowedURIchars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;="
    /** True if the argument string is in a form that can only be a file name */
    def looksLikeFilename(s: String): Boolean =
      (s.length >= 2 && s(0).isLetter && s(1) == ':') || // CP/M style drive letter
        !s.filterNot(allowedURIchars.contains(_)).isEmpty // Illegal URI chars
    def nameToURI(s: String): URI = {
      if (!looksLikeFilename(s)) new URI(s) else new File(s).toURI()
    }
    /** Ensure that "file:" URIs that refer to directories have a trailing slash.
      * This is necessary per URI parsing and resolution rules.
      */
    def slashifyDir(u: URI) = {
      if (u != null && u.getScheme != null && u.getScheme.equals("file")) new File(u).toURI() else u
    }
    pathElements.foldLeft(slashifyDir(baseURI))((x, y) => slashifyDir(x.resolve(nameToURI(y))))
  }

  def newInputFromPath(pathElements: String*): OrcInputContext = {
    val resolvedURI = resolve(this.toURI, pathElements: _*)
    OrcInputContext(resolvedURI)
  }

  override def toString = getClass().getCanonicalName() + "(descr=" + descr + ")"

  type CharacterNumber = TextPosition[OrcInputContext]#CharacterNumber
  type LineNumber = TextPosition[OrcInputContext]#LineNumber
  type ColumnNumber = TextPosition[OrcInputContext]#ColumnNumber

  private lazy val lineStartPosns: Array[CharacterNumber] = {
    val lineStartPosnsBuff = new ArrayBuffer[CharacterNumber]
    lineStartPosnsBuff += 0
    for (i <- 0 until reader.source.length) {
      /* Unicode line terminators are: CR, LF, CRLF, NEL[0085], LS[2028], FF, or PS[2029] */
      if ("\n\r\u0085\u2028\f\u2029".contains(reader.source.charAt(i))) {
        if (i < reader.source.length - 1 && reader.source.charAt(i) == '\r' && reader.source.charAt(i + 1) == '\n') {
          /* CRLF */
          lineStartPosnsBuff += (i + 2)
        } else {
          lineStartPosnsBuff += (i + 1)
        }
      }
    }
    lineStartPosnsBuff += reader.source.length
    lineStartPosnsBuff.toArray
  }

  def lineColForCharNumber(charNum: CharacterNumber): (LineNumber, ColumnNumber) = {
    require(charNum >= 0, "charNum nonnegative")
    require(charNum <= reader.source.length, "charNum <= input length")
    var lo = 0
    var hi = lineStartPosns.length - 1
    while (lo + 1 < hi) {
      val mid = (hi + lo) / 2
      if (charNum < lineStartPosns(mid)) hi = mid
      else lo = mid
    }
    (lo + 1, charNum - lineStartPosns(lo) + 1)
  }

  def lineText(startLineNum: LineNumber, endLineNum: LineNumber): String = {
    require(startLineNum > 0, "startLineNum positive")
    require(startLineNum < lineStartPosns.length, "startLineNum < input line count")
    require(endLineNum > 0, "endLineNum positive")
    require(endLineNum < lineStartPosns.length, "endLineNum < input line count")
    require(startLineNum <= endLineNum, "startLineNum <= endLineNum")
    stripLineEnd(reader.source.subSequence(lineStartPosns(startLineNum - 1), lineStartPosns(endLineNum)).toString)
  }

  def lineText(lineNum: LineNumber): String = {
    require(lineNum > 0, "lineNum positive")
    require(lineNum < lineStartPosns.length, "lineNum < input line count")
    lineText(lineNum, lineNum)
  }

  private def stripLineEnd(s: String): String = {
    if (s.endsWith("\r\n"))
      /* CRLF */
      s.dropRight(2)
    else if (!s.isEmpty() && "\n\r\u0085\u2028\f\u2029".contains(s.last))
      /* Other Unicode line terminators */
      s.dropRight(1)
    else
      s
  }

}

object OrcInputContext {
  // Factory method
  def apply(inputURI: URI): OrcInputContext = {
    inputURI.getScheme match {
      case "file" => new OrcFileInputContext(new File(inputURI), "UTF-8")
      case null => new OrcFileInputContext(new File(inputURI.getPath()), "UTF-8")
      case "data" => { val ssp = inputURI.getSchemeSpecificPart(); new OrcStringInputContext(ssp.drop(ssp.indexOf(',') + 1)) }
      //case "jar"  => { val ssp = inputURI.getSchemeSpecificPart(); new OrcResourceInputContext(ssp.drop(ssp.indexOf("!/")+1), ???) }
      case _ => new OrcNetInputContext(inputURI)
    }
  }
}

/** An OrcInputContext that reads from a given file.
  *
  * @author jthywiss
  */
class OrcFileInputContext(val file: File, val charsetName: String) extends OrcInputContext {
  override val descr: String = file.toString()
  override def toURI: URI = file.toURI
  override def toURL: URL = toURI.toURL
  override val reader: OrcReader = OrcReader(this, new InputStreamReader(new FileInputStream(file), charsetName))
}

/** An OrcInputContext that reads from a given string.
  *
  * @author jthywiss
  */
class OrcStringInputContext(val sourceString: String) extends OrcInputContext {
  override val descr: String = ""
  override def toURI: URI = new URI("data", "," + sourceString, null)
  override def toURL: URL = toURI.toURL
  override val reader: OrcReader = OrcReader(this, new StringReader(sourceString))
}

/** An OrcInputContext that reads from a JAR resource.
  *
  * @author jthywiss
  */
class OrcResourceInputContext(val resourceName: String, getResource: (String => URL)) extends OrcInputContext {
  override val descr: String = resourceName
  override def toURI: URI = toURL.toURI
  override def toURL: URL = getResource(resourceName)
  override val reader: OrcReader = {
    val r = toURL
    if (r == null) throw new IOException("Cannot open resource " + resourceName)
    OrcReader(this, new InputStreamReader(r.openStream()))
  }
}

/** An OrcInputContext that reads from a given network location (URI).
  *
  * @author jthywiss
  */
class OrcNetInputContext(val uri: URI) extends OrcInputContext {
  override val descr: String = uri.toString
  override def toURI: URI = uri
  override def toURL: URL = uri.toURL
  override val reader: OrcReader = OrcReader(this, new InputStreamReader(toURL.openStream, "UTF-8")) //URL.openStream returns a buffered SocketInputStream, so no additional buffering should be needed
}
