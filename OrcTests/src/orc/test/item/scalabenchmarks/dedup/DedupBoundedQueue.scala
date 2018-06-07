//
// DedupBoundedQueue.scala -- Scala benchmark DedupBoundedQueue
// Project OrcTests
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.dedup

import java.io.{ DataOutputStream, File, FileInputStream, FileOutputStream }
import java.util.concurrent.{ ArrayBlockingQueue, ConcurrentHashMap }

import scala.annotation.tailrec

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, BenchmarkConfig }
import orc.test.item.scalabenchmarks.Util.thread

object DedupBoundedQueue extends BenchmarkApplication[Unit, Unit] {
  import Dedup._
  
  def dedup(inFn: String, outFn: String): Unit = {
    val dedupMap = new ConcurrentHashMap[ArrayKey, CompressedChunk]()
    val roughChunks = new ArrayBlockingQueue[(Chunk, Int)](BenchmarkConfig.nPartitions)
    val fineChunks = new ArrayBlockingQueue[(Chunk, Int, Int)](BenchmarkConfig.nPartitions)
    val compressedChunks = new ArrayBlockingQueue[(CompressedChunk, Int, Int)](BenchmarkConfig.nPartitions)
        
    val in = new FileInputStream(inFn)
    
    val readThread = thread {
      for (p <- readSegments(largeChunkMin, in)) {
        roughChunks.put(p)   
      }
    }
    val segmentThreads = for (_ <- 0 until BenchmarkConfig.nPartitions / 2) yield thread {
      while (true) {
        val (roughChunk, roughID) = roughChunks.take()
        for ((fineChunk, fineID) <- segment(0, roughChunk)) {
          fineChunks.put((fineChunk, roughID, fineID))
        }
      }
    }
    val compressThreads = for (_ <- 0 until BenchmarkConfig.nPartitions / 2) yield thread {
      while (true) {
        val (fineChunk, roughID, fineID) = fineChunks.take()
        compressedChunks.put((compress(fineChunk, dedupMap), roughID, fineID))
      }
    }

    val out = new DataOutputStream(new FileOutputStream(outFn))
    val alreadyOutput = new ConcurrentHashMap[ArrayKey, Boolean]()
    val outputPool = collection.mutable.HashMap[(Int, Int), CompressedChunk]()
    
    @tailrec
    def doOutput(roughID: Int, fineID: Int, id: Int): Unit = {
      outputPool.get((roughID, fineID)) match {
        case Some(cchunk) if cchunk.uncompressedSize == 0 && fineID == 0 => {
          outputPool -= ((roughID, fineID))
        }
        case Some(cchunk) if cchunk.uncompressedSize == 0 => {
          outputPool -= ((roughID, fineID))
          doOutput(roughID + 1, 0, id)
        }
        case Some(cchunk) => {
          if (cchunk.outputChunkID < 0)
            cchunk.outputChunkID = id
    			writeChunk(out, cchunk, alreadyOutput.containsKey(cchunk.uncompressedSHA1))
    			alreadyOutput.put(cchunk.uncompressedSHA1, true)
    			//print(s"$id: ($roughID, $fineID) $roughChunk (${roughChunk.size}), $fineChunk (${fineChunk.size})\r")
          outputPool -= ((roughID, fineID))
          doOutput(roughID, fineID + 1, id + 1)
        }
        case None => {
          val (cchunk, rID, fID) = compressedChunks.take()
          outputPool += (rID, fID) -> cchunk
          doOutput(roughID, fineID, id)
        }
      }
    }
    
    doOutput(0, 0, 0)
    
    readThread.join()
    segmentThreads foreach { _.terminate() }
    compressThreads foreach { _.terminate() }
  }
  
  def benchmark(ctx: Unit): Unit = {
    dedup(DedupData.localInputFile, DedupData.localOutputFile)
  }

  def setup(): Unit = ()
  
  def check(u: Unit) = DedupData.check()

  val name: String = "Dedup-boundedqueue"

  lazy val size: Int = new File(DedupData.localInputFile).length().toInt
}
