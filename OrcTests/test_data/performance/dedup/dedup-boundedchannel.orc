{-
An implementation of the PARSEC 3.0 dedup benchmark.

This implementation uses Scala/Java classes for core computations but structures the entire computation
using Orc combinators. It does not use explicit channels making it totally different from the PARSEC 
implementation.

-}

include "benchmark.inc"

import class DedupData = "orc.test.item.scalabenchmarks.dedup.DedupData"
import class Rabin = "orc.test.item.scalabenchmarks.dedup.Rabin"
import class Chunk = "orc.test.item.scalabenchmarks.dedup.Chunk"
import class ArrayKey = "orc.test.item.scalabenchmarks.dedup.ArrayKey"
import class Map = "java.util.concurrent.ConcurrentHashMap"
import class File = "java.io.File"
import class FileInputStream = "java.io.FileInputStream"
import class FileOutputStream = "java.io.FileOutputStream"
import class DataOutputStream = "java.io.DataOutputStream"
import class MessageDigest = "java.security.MessageDigest"
import class Integer = "java.lang.Integer"

def logHalt(name, f) =
	val c = Channel() #
	(f() >v> Println(name + " published: " + v) >> c.put(v) >> stop ; Println(name + " halted") >> c.close() >> stop) |
	repeat(c.get)

class CompressedChunk {
  val compressedDataCell = Cell()
  
  val outputChunkID = Cell() 
  
  def compress(chunk) = compressedDataCell := chunk.deflate()
  
  def compressedData() = compressedDataCell?.buffer()
  
  val uncompressedSHA1
  val uncompressedSize
}
def CompressedChunk(s, n) = new CompressedChunk { val uncompressedSHA1 = s # val uncompressedSize = n }

val rabin = Rabin()
val largeChunkMin = 2 * 1024 * 1024
val readChunkSize = 128 * 1024 * 1024

def sha1(chunk) = ArrayKey(
	val m = MessageDigest.getInstance("SHA-1")
	m.update(chunk.buffer(), chunk.start(), chunk.size()) >>
	m.digest())

{-- Read chunks from an InputStream and publish chucks of it which are at least minimumSegmentSize long.  
-}
def readSegements(minimumSegmentSize, in, callback) =
	val _ = printLogLine("Start: readSegements")
	def process(currentChunk, i) =
		val splitPoint = rabin.segment(currentChunk, minimumSegmentSize)
		if splitPoint = currentChunk.size() then
			-- TODO: PERFORMANCE: This repeatedly reallocates a 128MB buffer. Even the JVM GC cannot handle that well, probably.
			Chunk.readFromInputStream(in, readChunkSize) >data>
			process(currentChunk.append(data), i) ;
			callback(currentChunk, i) >> callback(Chunk.empty(), i + 1) 
		else
			callback(currentChunk.slice(0, splitPoint), i) >>
			process(currentChunk.slice(splitPoint, currentChunk.size()), i+1)
	process(Chunk.empty(), 0) >> stop ;
	printLogLine("Done: readSegements")

	
{-- Publish some number of subchunks of chunk where each chunk is at least minimumSegmentSize long.  
-}
def segment(minimumSegmentSize, chunk, callback) =
	def process(chunk, i) if (chunk.size() = 0) = callback(Chunk.empty(), i)
	def process(chunk, i) =
		val splitPoint = rabin.segment(chunk, minimumSegmentSize) #
		callback(chunk.slice(0, splitPoint), i) >>
		process(chunk.slice(splitPoint, chunk.size()), i + 1)
	process(chunk, 0)
	
{-- Compress a chunk with deduplication by publishing an existing compressed chuck if an identical one exists.
-} 
def compress(chunk, dedupPool, id) =
	val hash = sha1(chunk)
	val old = dedupPool.putIfAbsent(hash, CompressedChunk(hash, chunk.size()))
	val compChunk = old >> dedupPool.get(hash)
	Ift(old = null) >> --printLogLine("Compressing CompressedChunk: " + compChunk.uncompressedSHA1) >> 
		compChunk.compress(chunk) >> stop |
	compChunk

def writeChunk(out, cchunk, isAlreadyOutput) =
	if isAlreadyOutput then
		--printLogLine("R chunk: " + (roughID, fineID) + cchunk.uncompressedSHA1) >>
		out.writeBytes("R") >> 
		out.writeLong(cchunk.outputChunkID?)
	else
		--printLogLine("D chunk: " + (roughID, fineID) + cchunk.uncompressedSHA1) >>
		out.writeBytes("D") >> 
		out.writeLong(cchunk.compressedData().length?) >>
		out.write(cchunk.compressedData())

{-- Read sequential elements from the pool and write to the provided OutputStream.
-}
def write(out, outputPool) =
	val _ = printLogLine("Start: write")
	val alreadyOutput = Map()
	def process((roughID, fineID), id) = 
		val cchunk = outputPool.get((roughID, fineID))
		if cchunk = null then
			--printLogLine("Pool: " + (roughID, fineID) + " " + outputPool) >>
			Rwait(100) >> process((roughID, fineID), id)
		else if cchunk.uncompressedSize = 0 then
			if fineID = 0 then
				printLogLine("Done") >>
				signal
			else
				process((roughID + 1, 0), id)
		else
			cchunk.outputChunkID := id >> stop |
			outputPool.remove((roughID, fineID)) >> stop |
			writeChunk(out, cchunk, alreadyOutput.containsKey(cchunk.uncompressedSHA1)) >>
			alreadyOutput.put(cchunk.uncompressedSHA1, true) >>
			process((roughID, fineID + 1), id + 1)
	process((0, 0), 0) >> stop ;
	printLogLine("Done: write")

{-- Connect the various stages using branch combinators
-}
def dedup(in, out) =
	val dedupPool = Map()
	val outputPool = Map()
	val roughChunks = BoundedChannel(4)
	val fineChunks = BoundedChannel(4)

	def fineSegment(roughChunk, roughID) =
		segment(0, roughChunk, { fineChunks.put((_, roughID, _)) })
	def fineSegmentThread() = 
		val _ = printLogLine("Start: segment")
		repeat({ roughChunks.get() >(roughChunk, roughID)> fineSegment(roughChunk, roughID) }) >> stop ;
		printLogLine("Done: segment") >> stop
		
	def compressThread() = 
		val _ = printLogLine("Start: compress")
		repeat({ fineChunks.get() >(fineChunk, roughID, fineID)> 
				 compress(fineChunk, dedupPool, (roughID, fineID)) >compressedChunk>
				 outputPool.put((roughID, fineID), compressedChunk) }) >> stop ;
		printLogLine("Done: compress") >> stop
		
	
	readSegements(largeChunkMin, in, lambda(c, i) = roughChunks.put((c, i))) >> stop |
	signals(nPartitions / 2) >> fineSegmentThread() |
	signals(nPartitions / 2) >> compressThread()  |
	write(out, outputPool) >>
	roughChunks.close() >>
	fineChunks.close()


benchmarkSized("Dedup-boundedchannel", File(DedupData.localInputFile()).length(), { signal }, lambda(_) =
  val (in, out) = (FileInputStream(DedupData.localInputFile()), DataOutputStream(FileOutputStream(DedupData.localOutputFile())))
  dedup(in, out) >>
  in.close() >>
  out.close()
)

{-
BENCHMARK
-}
