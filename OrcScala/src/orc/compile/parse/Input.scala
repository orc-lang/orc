//
// Input.scala -- Scala classes, traits, and objects relating to parser input sources
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 6, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.parse

import java.net.URI
import java.net.URL
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.StringReader
import java.io.IOException
import scala.collection.immutable.PagedSeq
import scala.util.parsing.input.Position
import scala.util.parsing.input.OffsetPosition
import scala.util.parsing.input.Reader
import scala.util.parsing.input.PagedSeqReader
import orc.util.FirstNonNull

/** Adds a filename field to scala.util.parsing.input.Position
  *
  * @author jthywiss
  */
trait PositionWithFilename extends Position {
  val filename: String
}

/** Position that understands files.
  * (Also fixes bug in OffsetPosition.lineContents)
  *
  * @author jthywiss
  */
class OrcPosition(source: java.lang.CharSequence, val filename: String, offset: Int) extends OffsetPosition(source, offset) with PositionWithFilename {

  override def toString = "" + filename + ":" + line + ":" + column

  override def <(that: Position) = {
    that match {
      case p: PositionWithFilename => {
        if (p.filename.equals(this.filename)) {
          super.<(p)
        } else {
          false //throw new IllegalArgumentException("Incomparable positions: "+this+" and "+that)
        }
      }
      case p => super.<(p)
    }
  }

  override def lineContents = super.lineContents.stripLineEnd

}

/** Reader implementation that has a filename (description) and uses OrcPosition for pos
  *
  * @author jthywiss
  */
class OrcReader(seq: PagedSeq[Char], val descr: String, offset: Int) extends PagedSeqReader(seq, offset) {
  if (seq == null) throw new NullPointerException("OrcReader.<init>(seq == null)")
  if (descr == null) throw new NullPointerException("OrcReader.<init>(descr == null)")

  override def rest: OrcReader =
    if (seq.isDefinedAt(offset)) new OrcReader(seq, descr, offset + 1)
    else this

  override def drop(n: Int): OrcReader =
    new OrcReader(seq, descr, offset + n)

  override def pos: OrcPosition = new OrcPosition(source, descr, offset)
}

object OrcReader {
  def apply(in: java.io.Reader, descr: String): OrcReader = new OrcReader(PagedSeq.fromReader(in), descr, 0)
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

  override def toString = getClass().getCanonicalName()+"(descr="+descr+")"
}

object OrcInputContext {
  // Factory method
  def apply(inputURI: URI): OrcInputContext = {
    inputURI.getScheme match {
      case "file" => new OrcFileInputContext(new File(inputURI), "UTF-8")
      case null => new OrcFileInputContext(new File(inputURI.getPath()), "UTF-8")
      case "data" => { val ssp = inputURI.getSchemeSpecificPart(); new OrcStringInputContext(ssp.drop(ssp.indexOf(',') + 1)) }
      //case "jar"  => { val ssp = inputURI.getSchemeSpecificPart(); new OrcResourceInputContext(ssp.drop(ssp.indexOf("!/")+1), ????) }
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
  override val reader: OrcReader = OrcReader(new BufferedReader(new InputStreamReader(new FileInputStream(file), charsetName)), descr)
}

/** An OrcInputContext that reads from a given string.
  *
  * @author jthywiss
  */
class OrcStringInputContext(val sourceString: String) extends OrcInputContext {
  override val descr: String = ""
  override def toURI: URI = new URI("data", "," + sourceString, null)
  override def toURL: URL = toURI.toURL
  override val reader: OrcReader = OrcReader(new StringReader(sourceString), descr)
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
    OrcReader(new InputStreamReader(r.openStream()), descr)
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
  override val reader: OrcReader = OrcReader(new InputStreamReader(toURL.openStream, "UTF-8"), descr) //URL.openStream returns a buffered SockentInputStream, so no additional buffering should be needed
}
