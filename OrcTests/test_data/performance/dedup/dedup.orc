{-
An implementation of the PARSEC 3.0 dedup benchmark.

This implementation uses Scala/Java classes for core computations but structures the entire computation
using Orc combinators. It does not use explicit channels making it totally different from the PARSEC or
Scala implementation.

-}

include "benchmark.inc"

import class Rabin = "orc.test.item.scalabenchmarks.dedup.Rabin"
import class Chunk = "orc.test.item.scalabenchmarks.dedup.Chunk"
import class ArrayKey = "orc.test.item.scalabenchmarks.dedup.ArrayKey"
import class Map = "java.util.concurrent.ConcurrentHashMap"
import class FileInputStream = "java.io.FileInputStream" 
import class FileOutputStream = "java.io.FileOutputStream" 
import class MessageDigest = "java.security.MessageDigest"
import class Integer = "java.lang.Integer" 

Need to implement nested numbering of some kind. That will provide IDs for the chunks.

class CompressedChunk {
  val compressedDataCell = Cell()  
  val outputChunkID = Cell()
  
  def compress(chunk) = compressedDataCell := chunk.deflate()
  
  val compressedData = compressedDataCell?.buffer()
  
  val uncompresseedSHA1
}
def CompressedChunk(s) = new CompressedChunk { val uncompresseedSHA1 = s }

val rabin = Rabin()
val largeChunkMin = 2 * 1024 * 1024
val readChunkSize = 128 * 1024 * 1024

{-- Read chunks from an InputStream and publish chucks of it which are at least minimumSegmentSize long.  
-}
def readSegements(minimumSegmentSize, in) =
	def process(currentChunk) =
		val splitPoint = rabin.segment(currentChunk, minimumSegmentSize)
		if splitPoint = currentChunk.size() then
			-- TODO: PERFORMANCE: This repeatedly reallocates a 128MB buffer. Even the JVM GC cannot handle that well, probably.
			process(currentChunk.append(Chunk.readFromInputStream(in, readChunkSize)))
		else
			currentChunk.slice(0, splitPoint) |
			process(currentChunk.slice(splitPoint, currentChunk.size()))
	process(Chunk.empty())

	
{-- Publish some number of subchunks of chunk where each chunk is at least minimumSegmentSize long.  
-}
def segment(_, chunk) if (chunk.size() = 0) = stop
def segment(minimumSegmentSize, chunk) =
	val splitPoint = rabin.segment(chunk, minimumSegmentSize)
	chunk.slice(0, splitPoint) |
	segment(minimumSegmentSize, chunk.slice(splitPoint, chunk.size()))
	
def compress(chunk, dedupPool) =
	val sha1 = ArrayKey(
		val m = MessageDigest.getInstance("SHA-1")
		m.update(chunk.buffer(), chunk.start(), chunk.size()) >>
		m.digest())
	val old = dedupPool.putIfAbsent(sha1, CompressedChunk(sha1))
	val compChunk = old >> dedupPool.get(sha1)
	Ift(old = null) >> Println("Compressing CompressedChunk: " + compChunk.uncompresseedSHA1) >> compChunk.compress(chunk) >> stop |
	compChunk

{-- Read sequential elements from the pool and write to the provided OutputStream.
-}
def write(out, outputPool) =
	val alreadyOutput = Map()
	def process(pos, id) = 
		val p = outputPool.get(pos)
		if p = null then
			Rwait(100) >> process(pos, id)
		else
			val (end, cchunk) = p
			cchunk.outputChunkID := id >> stop |
			outputPool.remove(pos) >> stop |
			(
			if alreadyOutput.containsKey(cchunk.uncompresseedSHA1) then
				Println("R chunk: " + cchunk.uncompresseedSHA1) >>
				out.write("R".getBytes("UTF-8")) >> 
				out.write(cchunk.uncompresseedSHA1.toString().getBytes("UTF-8"))
			else
				Println("D chunk: " + cchunk.uncompresseedSHA1) >>
				alreadyOutput.put(cchunk.uncompresseedSHA1, true) >>
				out.write("D".getBytes("UTF-8")) >> 
				out.write(Integer.toHexString(cchunk.compressedData.length?).getBytes("UTF-8")) >>
				out.write(cchunk.compressedData)
			) >>
			out.flush() >>
			process(end, id + 1)
	process(0, 0)

{-- Connect the various stages using branch combinators
-}
def dedup(in, out) =
	val outputPool = Map()
	val dedupPool = Map()
	readSegements(largeChunkMin, in) >roughChunk> --Println("Rough chunk: " + roughChunk.start() + " " + roughChunk.size()) >>
	segment(0, roughChunk) >chunk> --Println("Chunk: " + chunk.start() + " " + chunk.size()) >>
	compress(chunk, dedupPool) >compressedChunk>
	outputPool.put(chunk.start(), (chunk.end(), compressedChunk)) >> stop |
	write(out, outputPool)


benchmark({
  dedup(FileInputStream("test.in"), FileOutputStream("test.out"))
})