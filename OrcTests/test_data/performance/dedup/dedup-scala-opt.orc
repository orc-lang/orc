{-
An implementation of the PARSEC 3.0 dedup benchmark.

This implementation uses Scala/Java classes for core computations but structures the entire computation
using Orc combinators. It does not use explicit channels making it totally different from the PARSEC
implementation.

-}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

import class DedupData = "orc.test.item.scalabenchmarks.dedup.DedupData"
import class Rabin = "orc.test.item.scalabenchmarks.dedup.Rabin"
import class Chunk = "orc.test.item.scalabenchmarks.dedup.Chunk"
import class ArrayKey = "orc.test.item.scalabenchmarks.dedup.ArrayKey"

import class CompressedChunk = "orc.test.item.scalabenchmarks.dedup.CompressedChunkOrc"
import class DedupOrc = "orc.test.item.scalabenchmarks.dedup.DedupOrc"

import class Map = "java.util.concurrent.ConcurrentHashMap"
import class File = "java.io.File"
import class FileInputStream = "java.io.FileInputStream"
import class FileOutputStream = "java.io.FileOutputStream"
import class DataOutputStream = "java.io.DataOutputStream"
import class MessageDigest = "java.security.MessageDigest"
import class Integer = "java.lang.Integer" 

val rabin = Rabin()
val largeChunkMin = 2 * 1024 * 1024
val readChunkSize = 128 * 1024 * 1024

val sha1 = DedupOrc.sha1 

{-- Read chunks from an InputStream and publish chucks of it which are at least minimumSegmentSize long.  
-}
def readSegements(minimumSegmentSize, in) =
	def process(currentChunk, i) = (
		val splitPoint = rabin.segment(currentChunk, minimumSegmentSize)
		if splitPoint = currentChunk.size() then
			-- TODO: PERFORMANCE: This repeatedly reallocates a 128MB buffer. Even the JVM GC cannot handle that well, probably.
			Chunk.readFromInputStream(in, readChunkSize) >data>
			process(currentChunk.append(data), i) ;
			Sequentialize() >> 
			((currentChunk, i) | printLogLine("Done: readSegements " + (i + 1)) >> (Chunk.empty(), i + 1)) 
		else
			(currentChunk.slice(0, splitPoint), i) |
			process(currentChunk.slice(splitPoint, currentChunk.size()), i+1)
			)
	process(Chunk.empty(), 0)

	
{-- Publish some number of subchunks of chunk where each chunk is at least minimumSegmentSize long.  
-}
def segment(minimumSegmentSize, chunk) =
	def process(chunk, i) if (chunk.size() = 0) = {- printLogLine("segment " + i) >> -} (Chunk.empty(), i)
	def process(chunk, i) = Sequentialize() >> (
		val splitPoint = rabin.segment(chunk, minimumSegmentSize) #
		(chunk.slice(0, splitPoint), i) |
		process(chunk.slice(splitPoint, chunk.size()), i + 1)
		)
	process(chunk, 0)

{-- Compress a chunk with deduplication by publishing an existing compressed chuck if an identical one exists.
-} 
def compress(chunk, dedupPool, id) = (
	val hash = sha1(chunk)
	val old = dedupPool.putIfAbsent(hash, CompressedChunk(hash, chunk.size()))
	val compChunk = old >> dedupPool.get(hash)
	Ift(old = null) >> --printLogLine("Compressing CompressedChunk: " + compChunk.uncompressedSHA1) >> 
		compChunk.compress(chunk) >> stop |
	compChunk
	)

def getCompressedDataD(cchunk) =
    cchunk.compressedDataD() ; Rwait(100) >> getCompressedDataD(cchunk)


def writeChunk(out, cchunk, isAlreadyOutput) = (
	if isAlreadyOutput then
		--printLogLine("R chunk: " + (roughID, fineID) + cchunk.uncompressedSHA1) >>
		out.writeBytes("R") >> 
		out.writeLong(cchunk.getOutputChunkID())
	else
		--printLogLine("D chunk: " + (roughID, fineID) + cchunk.uncompressedSHA1) >>
		out.writeBytes("D") >> 
		getCompressedDataD(cchunk) >d>
		out.writeLong(d.length?) >>
		out.write(d)
	)

{-- Read sequential elements from the pool and write to the provided OutputStream.
-}
def write(out, outputPool) =
	val _ = printLogLine("Start: write")
	val alreadyOutput = Map()
	def process((roughID, fineID), id) = (
		val cchunk = outputPool.get((roughID, fineID))
		if cchunk = null then
			--printLogLine("Poll: " + (roughID, fineID) + " " + outputPool) >>
			Rwait(100) >> process((roughID, fineID), id)
		else if cchunk.uncompressedSize() = 0 then Sequentialize() >> (
			val _ = outputPool.remove((roughID, fineID))
			if fineID = 0 then
				printLogLine("Done") >>
				signal
			else
				process((roughID + 1, 0), id)
		)
		else Sequentialize() >> (
			val _ = outputPool.remove((roughID, fineID))
			--printLogLine("Write: " + (roughID, fineID) + " " + cchunk) >>
            cchunk.setOutputChunkID(id) >>
			writeChunk(out, cchunk, alreadyOutput.containsKey(cchunk.uncompressedSHA1())) >>
			alreadyOutput.put(cchunk.uncompressedSHA1(), true) >>
			process((roughID, fineID + 1), id + 1)
		)
		)
	process((0, 0), 0) >> stop ;
	printLogLine("Done: write")

{-- Connect the various stages using branch combinators
-}
def dedup(in, out) =
	val dedupPool = Map()
	val outputPool = Map() #
	readSegements(largeChunkMin, in) >(roughChunk, roughID)> 
	printLogLine("Rough chunk: " + roughID + ": " + roughChunk.start() + " " + roughChunk.size()) >>
	segment(0, roughChunk) >(chunk, fineID)> 
	(signal | (printLogLine("Chunk: " + (roughID, fineID) + " " + chunk.size()) >> stop)) >>
	compress(chunk, dedupPool, (roughID, fineID)) >compressedChunk> 
	(signal | (printLogLine("Compressed chunk: " + (roughID, fineID) + " " + compressedChunk.uncompressedSHA1() + " " + compressedChunk.compressedData()) >> stop)) >>
	outputPool.put((roughID, fineID), compressedChunk) >> stop |
	write(out, outputPool)


benchmarkSized("Dedup-scala-opt", File(DedupData.localInputFile()).length(), { signal }, lambda(_) =
  val (in, out) = (FileInputStream(DedupData.localInputFile()), DataOutputStream(FileOutputStream(DedupData.localOutputFile())))
  dedup(in, out) >>
  in.close() >>
  out.close(),
  { _ >> DedupData.check() }
)

{-
BENCHMARK
-}
