//
// Chunk.scala -- Scala class Chunk
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.dedup

import java.io.{ IOException, InputStream }
import java.util.zip.Deflater

/** A simple view on a chunk of a byte array.
  * 
  * The API is not polymorphic in anyway to make interfacing from Orc simple and reliable.
  */
case class Chunk(buffer: Array[Byte], start: Int, end: Int) {
  require(start >= 0)
  require(end <= buffer.length)
  
  def size = end - start
  
  def slice(a: Int, b: Int): Chunk = {
    require(a <= b)
    Chunk(buffer, start + a, start + b)
  }
  
  def appendArray(a: Array[Byte]): Chunk = {
    val newBuf = Array.ofDim[Byte](size + a.length)
    System.arraycopy(buffer, start, newBuf, 0, size)
    System.arraycopy(a, 0, newBuf, size, a.length)
    Chunk(newBuf, 0, newBuf.length)
  }
  
  def append(a: Chunk): Chunk = {
    val newBuf = Array.ofDim[Byte](size + a.size)
    System.arraycopy(buffer, start, newBuf, 0, size)
    System.arraycopy(a.buffer, a.start, newBuf, size, a.size)
    Chunk(newBuf, 0, newBuf.length)
  }
  
  def compact = {
    if (size < buffer.length) {
      val newBuf = Array.ofDim[Byte](size)
      System.arraycopy(buffer, start, newBuf, 0, size)
      Chunk(newBuf, 0, newBuf.length)
    } else {
      this
    }
  }
  
  def deflate() = {
    val deflater = new Deflater(5)
    deflater.setInput(buffer, start, size)
    val out = Array.ofDim[Byte]((size * 1001) / 1000 + 12)
    deflater.finish()
    val n = deflater.deflate(out)
    val r = Chunk(out, 0, n)
    deflater.end()
    r.compact
  }
}

object Chunk {
  val empty = Chunk(Array.emptyByteArray, 0, 0)
  
  def fromArray(a: Array[Byte]) = {
    Chunk(a, 0, a.length)
  }
  
  def readFromInputStream(in: InputStream, n: Int): Chunk = {
    val a = Array.ofDim[Byte](n)
    val nRead = in.read(a)
    if (nRead > 0) {
      Chunk(a, 0, nRead).compact
    } else {
      throw new IOException("End of file reached")
    }
  }
}
