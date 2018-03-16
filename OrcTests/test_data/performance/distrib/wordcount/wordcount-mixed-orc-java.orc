{- wordcount-mixed-orc-java.orc -- Count words in text files, Mixed Orc-Java variant -}

{- This performance test counts words in a set of plain text files.  The
 - wordcount-input-data directory is walked, and a list of files is created.
 - The {{{repeatCountFilename}}} def is applied to each file in the list.
 - This def uses our WordCount utility Java class {{{repeatRead}}} times to
 - count the number of words in the given file.  A list of word counts (times
 - {{{repeatRead}}}) for each file is the result.  These counts are summed
 - using Orc's associative fold library function (afold).
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
  WordCount.countReader(in)  >count>
  in.close()  >>
  count

def repeatCountFilename(filename) =
  import class File = "java.io.File"
  def sumN(n, f) = if (n :> 0) then f() + sumN(n-1, f) else 0

  File(filename)  >file>
  checkReadableFile(file)  >>
  sumN(repeatRead, { countFile(file) })

def listFileNamesRecursively(dirPathName :: String) :: List[String] =
  import class File = "java.io.File"
  import class WordCount = "orc.test.item.distrib.WordCount"
  WordCount.listFileNamesRecursively(File(dirPathName))  >fileNameArray>
  arrayToList(fileNameArray)

val inputList = take(numInputFiles, listFileNamesRecursively("../OrcTests/test_data/performance/distrib/wordcount/wordcount-input-data/"))

def testPayload() =
  map(repeatCountFilename, inputList)  >wordCountList>
  afold((+), wordCountList)


{--------
 - Test Driver
 --------}

val numRepetitions = Read(JavaSys.getProperty("orc.test.numRepetitions", "20"))

def getProcessCumulativeCpuTime() =
  import class ManagementFactory = "java.lang.management.ManagementFactory"
  ManagementFactory.getOperatingSystemMXBean().getProcessCpuTime()

def timeRepetitions(testPayload, numRepetitions) =
  def timeRepetitions'(thisRepetitionNum, remainingRepetitions, testElapsedTimes) =
	Println("Repetition " + thisRepetitionNum + ": start.") >>
	JavaSys.nanoTime() >startElapsed_ns>
	getProcessCumulativeCpuTime()  >startCpuTime_ns>
	(testPayload() >p> Println("Repetition " + thisRepetitionNum + ": published " + p) >> stop; signal) >>
	getProcessCumulativeCpuTime()  >finishCpuTime_ns>
	JavaSys.nanoTime() >finishElapsed_ns>
	(finishElapsed_ns - startElapsed_ns) / 1000  >elapsed_us>
    (finishCpuTime_ns - startCpuTime_ns) / 1000000  >cpuTime_ms>	
	Println("Repetition " + thisRepetitionNum + ": finish.  Elapsed time " + elapsed_us + " µs, CPU time " + cpuTime_ms + " ms") >>
	append(testElapsedTimes, [[thisRepetitionNum, elapsed_us, cpuTime_ms]])  >testElapsedTimes'>
	(if remainingRepetitions :> 0 then timeRepetitions'(thisRepetitionNum + 1, remainingRepetitions - 1, testElapsedTimes') else testElapsedTimes')
  timeRepetitions'(1, numRepetitions - 1, [])

import site NumberOfRuntimeEngines = "orc.lib.NumberOfRuntimeEngines"

setupOutput()  >>
writeFactorValuesTable([
  --Factor name, Value, Units, ID, Comments
  ("Program", "wordcount-mixed-orc-java.orc", "", "", ""),
  ("Number of files read", length(inputList), "", "numInputFiles", "Words counted in this number of input text files"),
  ("Reads per file", repeatRead, "", "repeatRead", "Number of concurrent reads of the file"),
  ("Cluster size", NumberOfRuntimeEngines(), "", "dOrcNumRuntimes", "Number of d-Orc runtime engines running")
])  >>
timeRepetitions(testPayload, numRepetitions)  >repetitionTimes>
writeCsvFile(buildOutputPathname("repetition-times", "csv"), "Repetitions' elapsed times output file",
  ["Repetition number", "Elapsed time (µs)", "CPU time (ms)"], repetitionTimes)  >>
repetitionTimes


{-
OUTPUT:
Repetition ...: start.
Repetition ...: published ...
Repetition ...: finish.  Elapsed time ... µs, CPU time ... ms
......
Repetitions' elapsed times output file written to ...
[[..., ...], ......]
-}
