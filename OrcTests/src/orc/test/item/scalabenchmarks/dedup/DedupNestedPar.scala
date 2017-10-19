package orc.test.item.scalabenchmarks.dedup

import java.io.{ DataOutputStream, FileInputStream, FileOutputStream }
import java.util.concurrent.ConcurrentHashMap

import orc.test.item.scalabenchmarks.{ BenchmarkApplication, Util }
import java.util.concurrent.ArrayBlockingQueue
import Util.thread
import scala.annotation.tailrec
import java.io.File

object DedupNestedPar extends BenchmarkApplication[Unit] {
  import Dedup._
  
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
  }
  
  def benchmark(ctx: Unit): Unit = {
    dedup(DedupData.localInputFile, DedupData.localOutputFile)
  }

  def setup(): Unit = ()

  val name: String = "Dedup-nestedpar"

  val size: Int = new File(DedupData.localInputFile).length().toInt
}