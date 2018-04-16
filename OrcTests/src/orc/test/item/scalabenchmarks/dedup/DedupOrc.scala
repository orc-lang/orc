//
// Dedup.scala -- Scala benchmark Dedup
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.dedup

import java.security.MessageDigest

import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration.Duration

case class CompressedChunkOrc(uncompressedSHA1: ArrayKey, uncompressedSize: Int) {
  private var outputChunkID: Int = -1
  def setOutputChunkID(i: Int) = synchronized {
    if (outputChunkID < 0) { 
      outputChunkID = i
    }
  }
  def getOutputChunkID(): Int = synchronized {
    outputChunkID
  }
  
  private val compressPromise = Promise[Array[Byte]]()
  def compressedData() = Await.result(compressPromise.future, Duration.Inf)
  def compressedDataD() = compressPromise.future.value.get.get
  def compress(chunk: Chunk): Unit = {
    compressPromise.trySuccess(chunk.deflate().buffer)
  }
}

object DedupOrc {
  def sha1(chunk: Chunk): ArrayKey = new ArrayKey({
  	val m = MessageDigest.getInstance("SHA-1")
  	m.update(chunk.buffer, chunk.start, chunk.size)
  	m.digest()
	})

  val rabin = new Rabin()
  val largeChunkMin = 2 * 1024 * 1024
  val readChunkSize = 128 * 1024 * 1024
}
