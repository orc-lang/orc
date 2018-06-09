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

import java.io.{ DataOutputStream, File, FileInputStream, FileOutputStream, IOException }
import java.security.MessageDigest
import java.util.concurrent.{ ConcurrentHashMap, ForkJoinPool }

import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration.Duration

import orc.test.item.scalabenchmarks.BenchmarkApplication

object Dedup extends BenchmarkApplication[Unit, Unit] {
  val threadPool = new ForkJoinPool()
  
  // FIXME: This does not close the output file (or input file) which means it doesn't wait for the output to be flushed out of even internal buffers.
  
  case class CompressedChunk(uncompressedSHA1: ArrayKey, uncompressedSize: Int) {
    var outputChunkID: Int = -1
    private val compressPromise = Promise[Array[Byte]]()
    def compressedData() = Await.result(compressPromise.future, Duration.Inf)
    def compress(chunk: Chunk): Unit = {
      threadPool.execute(() => {
        compressPromise.trySuccess(chunk.deflate().buffer)
      })
    }
  }
  
  val rabin = new Rabin()
  val largeChunkMin = 2 * 1024 * 1024
  val readChunkSize = 128 * 1024 * 1024

  def sha1(chunk: Chunk): ArrayKey = new ArrayKey({
  	val m = MessageDigest.getInstance("SHA-1")
  	m.update(chunk.buffer, chunk.start, chunk.size)
  	m.digest()
	})

  def readSegments(minimumSegmentSize: Int, in: FileInputStream): Stream[(Chunk, Int)] = {
    //@tailrec
  	def process(currentChunk: Chunk, i: Int): Stream[(Chunk, Int)] = {
  		val splitPoint = rabin.segment(currentChunk, minimumSegmentSize)
  		if (splitPoint == currentChunk.size) {
  		  try {
    			val data = Chunk.readFromInputStream(in, readChunkSize)
    			// TODO: PERFORMANCE: This repeatedly reallocates a 128MB buffer. Even the JVM GC cannot handle that well, probably.
    			process(currentChunk.append(data), i)
  		  } catch {
  		    case _: IOException =>
  			    Stream((currentChunk, i), (Chunk.empty, i + 1))
  		  }
  		} else {
  			(currentChunk.slice(0, splitPoint), i) #::
    			process(currentChunk.slice(splitPoint, currentChunk.size), i+1)
  		}
    }
    process(Chunk.empty, 0)
  }
  
  def segment(minimumSegmentSize: Int, chunk: Chunk): Stream[(Chunk, Int)] = {
    //@tailrec
  	def process(chunk: Chunk, i: Int): Stream[(Chunk, Int)] = {
  	  if (chunk.size == 0) {
  	    Stream((Chunk.empty, i))
  	  } else {
    		val splitPoint = rabin.segment(chunk, minimumSegmentSize)
    		(chunk.slice(0, splitPoint), i) #::
      		process(chunk.slice(splitPoint, chunk.size), i + 1)
  	  }
  	}
  	process(chunk, 0)
  }
  
  def compress(chunk: Chunk, dedupPool: ConcurrentHashMap[ArrayKey, CompressedChunk]) = {
  	val hash = sha1(chunk)
  	val old = dedupPool.putIfAbsent(hash, CompressedChunk(hash, chunk.size))
  	val compChunk = dedupPool.get(hash)
  	if (old == null)
  		compChunk.compress(chunk)
  	compChunk
  }
  def writeChunk(out: DataOutputStream, cchunk: CompressedChunk, isAlreadyOutput: Boolean) = {
  	if (isAlreadyOutput) {
  		out.writeBytes("R")
  		out.writeLong(cchunk.outputChunkID)
  	} else {
  		out.writeBytes("D")
  		out.writeLong(cchunk.compressedData().length)
  		out.write(cchunk.compressedData())
  	}
  }
  
  def dedup(inFn: String, outFn: String): Unit = {
    val dedupMap = new ConcurrentHashMap[ArrayKey, CompressedChunk]()
    val alreadyOutput = new ConcurrentHashMap[ArrayKey, Boolean]()
    var id = 0
    
    val in = new FileInputStream(inFn)
    val out = new DataOutputStream(new FileOutputStream(outFn))
    val cchunks = for { 
      (roughChunk, roughID) <- readSegments(largeChunkMin, in) 
      (fineChunk, fineID) <- segment(0, roughChunk)
    } yield (roughChunk, roughID, fineChunk, fineID, compress(fineChunk, dedupMap))
    
    for ((roughChunk, roughID, fineChunk, fineID, cchunk) <- cchunks if cchunk.uncompressedSize != 0) {
      cchunk.outputChunkID = id
      id += 1
			writeChunk(out, cchunk, alreadyOutput.containsKey(cchunk.uncompressedSHA1))
			alreadyOutput.put(cchunk.uncompressedSHA1, true)
			//print(s"$id: ($roughID, $fineID) $roughChunk (${roughChunk.size}), $fineChunk (${fineChunk.size})\r")
    }
  }

  def benchmark(ctx: Unit): Unit = {
    dedup(DedupData.localInputFile, DedupData.localOutputFile)
  }

  def setup(): Unit = ()
  
  def check(u: Unit) = DedupData.check()

  val name: String = "Dedup"

  val size: Int = new File(DedupData.localInputFile).length().toInt
}
