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

{- Number of files to read. -}
val numInputFiles = Read(JavaSys.getProperty("orc.test.numInputFiles", "12"))

{- Number of times to re-read and word count each file. -}
val repeatRead = Read(JavaSys.getProperty("orc.test.repeatRead", "1"))

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
  import class File = "java.io.File"
  -- Run n copies of f to build a list.
  def loop(0, f) = []
  def loop(1, f) = [f()]
  def loop(n, f) = {| f() |} : loop(n-1, f) 

  File(filename)  >file>
  checkReadableFile(file)  >>
  loop(repeatRead,
    { countFile(file) }
  )

def combineOperation(xs) = afold((+), xs)

def reduceOperation(x, y) = x + y

def listFileNamesRecursively(dirPathName :: String) :: List[String] =
  import class File = "java.io.File"
  import class WordCount = "orc.test.item.distrib.WordCount"
  WordCount.listFileNamesRecursively(File(dirPathName))  >fileNameArray>
  arrayToList(fileNameArray)

val inputList = take(numInputFiles, listFileNamesRecursively("../OrcTests/test_data/performance/distrib/holmes_test_data/"))

def testPayload() =
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
  --Factor name, Value, Units, ID, Comments
  ("Program", "holmes-map-reduce-java.orc", "", "", ""),
  ("Number of files read", length(inputList), "", "numInputFiles", "Words counted in this number of input text files"),
  ("Reads per file", repeatRead, "", "repeatRead", "Number of concurrent reads of the file"),
  ("Cluster size", NumberOfRuntimeEngines(), "", "dOrcNumRuntimes", "Number of d-Orc runtime engines running")
])  >>
timeRepetitions(testPayload, numRepetitions)  >repetitionTimes>
writeCsvFile(buildOutputPathname("repetition-times", "csv"), "Repetitions' elapsed times output file",
  ["Repetition number", "Elapsed time (µs)"], repetitionTimes)  >>
repetitionTimes


{-
OUTPUT:
Repetitions' elapsed times output file written to...
-}
