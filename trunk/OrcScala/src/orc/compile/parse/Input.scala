//
// Input.scala -- Scala class/trait/object Input
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 6, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.parse

import scala.collection.immutable.PagedSeq
import scala.util.parsing.input.Position
import scala.util.parsing.input.OffsetPosition
import scala.util.parsing.input.Reader
import scala.util.parsing.input.PagedSeqReader

/**
 * Adds a filename field to scala.util.parsing.input.Position
 *
 * @author jthywiss
 */
trait PositionWithFilename extends Position {
  val filename: String
}

/**
 * Position that understands files.
 * (Also fixes bug in OffsetPosition.lineContents)
 *
 * @author jthywiss
 */
class OrcPosition(source: java.lang.CharSequence, val filename: String, offset: Int) extends OffsetPosition(source, offset) with PositionWithFilename {
  override def toString = ""+filename+":"+line+":"+column
  
  override def <(that: Position) = {
    that match {
      case p: PositionWithFilename => { 
        if (p.filename.equals(this.filename)) {
          super.<(p)
        } else {
          false//throw new IllegalArgumentException("Incomparable positions: "+this+" and "+that)
        }
      }
      case p => super.<(p)
    }
  }
  
  override def lineContents = super.lineContents.stripLineEnd

}

/**
 * Reader that has a filename (description) and can create new readers with a relative filename
 *
 * @author jthywiss
 */
trait NamedSubfileReader[T] extends Reader[T] {
  val descr: String;
  def newSubReader(newFilename: String): NamedSubfileReader[Char]
}

/**
 * Function that converts filenames into paged character sequences
 *
 * @author jthywiss
 */
trait NameToCharSeq {
  def apply(newFilename: String): (PagedSeq[Char], NameToCharSeq)
}


/**
 * Reader implementation that has a filename (description) and can create new readers with a relative filename
 *
 * @author jthywiss
 */
class OrcReader(seq: PagedSeq[Char], val descr: String, nameToPagedSeq: NameToCharSeq, offset: Int) extends PagedSeqReader(seq, offset) with NamedSubfileReader[Char] {

  override def rest: OrcReader =
    if (seq.isDefinedAt(offset)) new OrcReader(seq, descr, nameToPagedSeq, offset + 1)
    else this

  override def drop(n: Int): OrcReader = 
    new OrcReader(seq, descr, nameToPagedSeq, offset + n)

  override def pos: OrcPosition = new OrcPosition(source, descr, offset)

  def newSubReader(newFilename: String): OrcReader = {
      val (newSeq, newFileToSeq) = nameToPagedSeq(newFilename) 
      new OrcReader(newSeq, newFilename, newFileToSeq, 0)
    }
}

object OrcReader {
  def fileNameToCharSeq(startingPointName: String, openInclude: (String, String) => java.io.Reader): NameToCharSeq = new NameToCharSeq {
    def apply (newFilename: String) = {
      val relToPath = (new java.io.File(startingPointName)).getParent()
      val includeReader = openInclude(newFilename, relToPath)
      val incfile = new java.io.File(relToPath, newFilename)
      (PagedSeq.fromReader(includeReader), fileNameToCharSeq(incfile.toString, openInclude))
    }
  }

  def apply(in: java.io.Reader, name: String, openInclude: (String, String) => java.io.Reader): OrcReader = new OrcReader(PagedSeq.fromReader(in), name, fileNameToCharSeq(name, openInclude), 0)
}
