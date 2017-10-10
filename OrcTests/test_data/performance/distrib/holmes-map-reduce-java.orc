{- holmes-map-reduce-java.orc -- A d-Orc map-reduce -}

{- This performance test counts words in the 12 data files adventure-*.txt.
 - Each file is counted {{{repeatRead}}} times.  The file is processed by
 - this test's WordCount Java class, which results in, for each file, a list
 - (per-iteration) of file word counts.  The multiple iterations of one file
 - are combined by folding the lists with the + operator.  Then the resulting
 - per-file word counts (times {{{repeatRead}}}) are reduced, again by
 - folding with the + operator. The folds use Orc's associative fold library
 - function (afold).
 -}

include "test-output-util.inc"
include "write-csv-file.inc"

import class JavaSys = "java.lang.System"

{- Number of times to re-read and word count each file. -}
{- Update the OUTPUT annotation when changing this. -}
val repeatRead = Read(JavaSys.getProperty("orc.test.repeatRead", "3"))

def checkReadableFile(file) =
  import class JavaSys = "java.lang.System"
  if file.canRead() then signal else Error("Cannot read file: "+file+" in dir "+JavaSys.getProperty("user.dir")) >> stop

def countFile(file) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  import class WordCount = "orc.test.item.distrib.WordCount"
  BufferedReader(FileReader(file))  >in>
  WordCount.countReader(in)  >counts>
  in.close()  >>
  counts

def mapOperation(filename) =
  -- Run n copies of f to build a list.
  def loop(0, f) = []
  def loop(1, f) = [f()]
  def loop(n, f) = {| f() |} : loop(n-1, f) 

  import class File = "java.io.File"
  File(filename)  >f>
  checkReadableFile(f)  >>
  loop(repeatRead,
    { countFile(f) }
  )

def combineOperation(xs) = afold((+), xs)

def reduceOperation(x, y) = x + y

def testPayload() =
"../OrcTests/test_data/functional_valid/distrib/holmes_test_data/"  >dataDir>

[
  dataDir + "adventure-1.txt",
  dataDir + "adventure-2.txt",
  dataDir + "adventure-3.txt",
  dataDir + "adventure-4.txt",
  dataDir + "adventure-5.txt",
  dataDir + "adventure-6.txt",
  dataDir + "adventure-7.txt",
  dataDir + "adventure-8.txt",
  dataDir + "adventure-9.txt",
  dataDir + "adventure-10.txt",
  dataDir + "adventure-11.txt",
  dataDir + "adventure-12.txt"
] >inputList>

map(mapOperation, inputList)  >mappedList>
map(combineOperation, mappedList)  >combinedList>
afold(reduceOperation, combinedList)


{--------
 - Test Driver
 --------}
 
val numRepetitions = Read(JavaSys.getProperty("orc.test.numRepetitions", "20"))

def timeRepetitions(testPayload, numRepetitions) =
  def timeRepetitions'(thisRepetitionNum, remainingRepetitions, testElapsedTimes) =
	Println("Repetition " + thisRepetitionNum + ": start.") >>
	JavaSys.nanoTime() >startNanos>
	(testPayload() >p> Println("Repetition " + thisRepetitionNum + ": published " + p) >> stop; signal) >>
	JavaSys.nanoTime() >finishNanos>
	Println("Repetition " + thisRepetitionNum + ": finish.  Elapsed time " + (finishNanos - startNanos)/1000 + " µs") >>
	append(testElapsedTimes, [[thisRepetitionNum, (finishNanos - startNanos)/1000]])  >testElapsedTimes'>
	(if remainingRepetitions :> 0 then timeRepetitions'(thisRepetitionNum + 1, remainingRepetitions - 1, testElapsedTimes') else testElapsedTimes')
  timeRepetitions'(1, numRepetitions - 1, [])

import site NumberOfRuntimeEngines = "orc.lib.NumberOfRuntimeEngines"

setupOutput()  >>
writeFactorValuesTable([
  --Factor name, Value, Units, Comments
  ("Program", "holmes-map-reduce-java.orc", "", ""),
  ("Reads per file", repeatRead, "", "Number of concurrent reads of the file"),
  ("Cluster size", NumberOfRuntimeEngines(), "", "Number of d-Orc runtime engines running")
])  >>
timeRepetitions(testPayload, numRepetitions)  >repetitionTimes>
writeCsvFile(buildOutputPathname("repetition-times", "csv"), "Repetitions' elapsed times output file",
  ["Repetition number", "Elapsed time (µs)"], repetitionTimes)  >>
repetitionTimes


{-
OUTPUT:
Repetitions' elapsed times output file written to...
-}
