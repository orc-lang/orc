//
// DedupNestedPar.scala -- Scala benchmark DedupNestedPar
// Project OrcTests
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.scalabenchmarks.dedup

import java.io.{ DataOutputStream, FileInputStream, FileOutputStream }
import java.nio.file.{ Files, Paths }
import java.util.concurrent.{ ArrayBlockingQueue, ConcurrentHashMap }

import scala.annotation.tailrec

import orc.test.item.scalabenchmarks.BenchmarkApplication
import orc.test.item.scalabenchmarks.Util.thread

// FIXME: This is not scaling and it's not clear why. Work just seems to execute pretty sparsely.
//   GC overhead is a bit of an issue (7% of the runtime), but it doesn't seem to be affecting scaling in a huge way.

object DedupNestedPar extends BenchmarkApplication[Unit, Unit] {
  import Dedup._

  // Lines: 33 (2)
  def dedup(inFn: String, outFn: String): Unit = {
    val dedupMap = new ConcurrentHashMap[ArrayKey, CompressedChunk]()
    val compressedChunks = new ArrayBlockingQueue[(CompressedChunk, Int, Int)](2 * 1024)

    val in = new FileInputStream(inFn)

    val loopThread = thread {
      for {
        (roughChunk, roughID) <- readSegments(largeChunkMin, in).par
        (fineChunk, fineID) <- segment(0, roughChunk).par
      } {
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

    loopThread.join()

    in.close()
    out.close()
  }

  def benchmark(ctx: Unit): Unit = {
    dedup(DedupData.localInputFile, DedupData.localOutputFile)
  }

  def setup(): Unit = ()

  def check(u: Unit) = DedupData.check()

  val name: String = "Dedup-nestedpar"

  lazy val size: Int = Files.size(Paths.get(DedupData.localInputFile)).toInt
}
