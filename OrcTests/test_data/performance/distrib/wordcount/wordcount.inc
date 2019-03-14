{- wordcount.inc -- Orc include: Word count tests common defintions
 -
 - Created by jthywiss on Apr 5, 2018 13:37
 -}

include "test-output-util.inc"
include "write-csv-file.inc"

import class JavaSys = "java.lang.System"

{- Number of files to read. -}
val numInputFiles = Read(JavaSys.getProperty("orc.test.numInputFiles", "12"))

{- Number of times to re-read and word count each file. -}
val repeatRead = Read(JavaSys.getProperty("orc.test.repeatRead", "1"))

val holmesDataDirPath = "../OrcTests/test_data/functional_valid/distrib/holmes_test_data"

val inputDataDirPath = "../OrcTests/test_data/performance/distrib/wordcount/wordcount-input-data/"

val targetFileSize = 17895697 {- bytes -} -- = 2 GiB / 120

val numCopiesInputFiles = 130 -- can be numInputFiles, if we delete and re-gen input files for every condition

val inputFileSize = Cell[Integer]()


{--------
 - I/O Utilities
 --------}

def checkReadableFile(file) =
  if file.canRead() then signal else Error("Cannot read file: "+file+" in dir "+JavaSys.getProperty("user.dir")) >> stop

def listFileNamesRecursively(dirPathName :: String) :: List[String] =
  import class File = "java.io.File"
  import class WordCount = "orc.test.item.distrib.WordCount"
  WordCount.listFileNamesRecursively(File(dirPathName))  >fileNameArray>
  arrayToList(fileNameArray)

def createTestDataFiles() =
  import class WordCount = "orc.test.item.distrib.WordCount"
  inputFileSize :=  WordCount.createTestFiles(holmesDataDirPath, inputDataDirPath, targetFileSize, numCopiesInputFiles)

def deleteTestDataFiles() =
  import class WordCount = "orc.test.item.distrib.WordCount"
  WordCount.deleteTestFiles(inputDataDirPath, numCopiesInputFiles)


{--------
 - Test Driver
 --------}

val numRepetitions = Read(JavaSys.getProperty("orc.test.numRepetitions", "20"))

def getProcessCumulativeCpuTime() =
  import class ManagementFactory = "java.lang.management.ManagementFactory"
  ManagementFactory.getOperatingSystemMXBean().getProcessCpuTime()

def timeRepetitions(setUpTestRep, runTestRep, tearDownTestRep, numRepetitions) =
  def timeRepetitions'(thisRepetitionNum, remainingRepetitions, testElapsedTimes) =
    Println("Repetition " + thisRepetitionNum + ": setting up.")  >>
    (setUpTestRep() >> stop; signal)  >>
    Println("Repetition " + thisRepetitionNum + ": start run.")  >>
    JavaSys.nanoTime()  >startElapsed_ns>
    getProcessCumulativeCpuTime()  >startCpuTime_ns>
    (runTestRep() >p> Println("Repetition " + thisRepetitionNum + ": published " + p) >> stop; signal)  >>
    getProcessCumulativeCpuTime()  >finishCpuTime_ns>
    JavaSys.nanoTime()  >finishElapsed_ns>
    (finishElapsed_ns - startElapsed_ns) / 1000  >elapsed_us>
    (finishCpuTime_ns - startCpuTime_ns) / 1000000  >cpuTime_ms>
    Println("Repetition " + thisRepetitionNum + ": finish run.  Elapsed time " + elapsed_us + " µs, leader CPU time " + cpuTime_ms + " ms")  >>
    append(testElapsedTimes, [[thisRepetitionNum, elapsed_us, cpuTime_ms]])  >testElapsedTimes'>
    Println("Repetition " + thisRepetitionNum + ": tearing down.")  >>
    (tearDownTestRep() >> stop; signal)  >>
    (if remainingRepetitions :> 0 then timeRepetitions'(thisRepetitionNum + 1, remainingRepetitions - 1, testElapsedTimes') else testElapsedTimes')
  timeRepetitions'(1, numRepetitions - 1, [])

import site NumberOfRuntimeEngines = "orc.lib.NumberOfRuntimeEngines"

def executeTest(programName, setUpTest, setUpTestRep, runTestRep, tearDownTestRep, tearDownTest) =
  setupOutput()  >>
  Println("Test start. Setting up test.")  >>
  (setUpTest() >> stop; signal)  >>
  take(numInputFiles, listFileNamesRecursively(inputDataDirPath))  >inputList>
  writeFactorValuesTable([
    --Factor name, Value, Units, ID, Comments
    ("Program", programName, "", "", ""),
    ("Number of files read", length(inputList), "", "numInputFiles", "Words counted in this number of input text files"),
    ("Input file size", inputFileSize?, "B", "inputFileSize", "Size of each input text file"),
    ("Reads per file", repeatRead, "", "repeatRead", "Number of concurrent reads of the file"),
    ("Cluster size", NumberOfRuntimeEngines(), "", "dOrcNumRuntimes", "Number of d-Orc runtime engines running")
  ])  >>
  timeRepetitions(setUpTestRep, { runTestRep(inputList) }, tearDownTestRep, numRepetitions)  >repetitionTimes>
  writeCsvFile(buildOutputPathname("repetition-times", "csv"), "Repetitions' elapsed times output file",
    ["Repetition number", "Elapsed time (µs)", "Leader CPU time (ms)"], repetitionTimes)  >>
  Println("Tearing down test.")  >>
  (tearDownTest() >> stop; signal)  >>
  Println("Test finish.")  >>
  repetitionTimes
