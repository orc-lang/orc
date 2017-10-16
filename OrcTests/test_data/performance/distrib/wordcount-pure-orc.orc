{- wordcount-pure-orc.orc -- Count words in text files, Pure Orc variant -}

{- This performance test counts words in the text files in holmes_test_data.
 - The holmes_test_data directory is walked, and a list of files is created.
 - The {{{repeatCountFilename}}} def is applied to each file in the list.
 - This def uses the {{{countFile}}} def {{{repeatRead}}} times to count the
 - the number of words in the given file.  A list of word counts (times
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

def countLine(line) =
  import class BreakIterator = "java.text.BreakIterator"
  import class Character = "java.lang.Character"
  def wordCount'(wb, line) =
    def containsAlphabetic(s, startPos, endPos) =
      Character.isAlphabetic(s.codePointAt(startPos)) || (if startPos+1 <: endPos then containsAlphabetic(s, startPos+1, endPos) else false)
    def wordCount''(startPos) =
      wb.next()  >endPos>
      (if endPos <: 0 then 0 else (if containsAlphabetic(line, startPos, endPos) then 1 else 0) + wordCount''(endPos)) #
    wb.setText(line)  >>
    wordCount''(0)
  BreakIterator.getWordInstance() >wb>
  wordCount'(wb, line)

def countFile(file) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  def countLinesFrom(in) =
    (in.readLine() ; null)  >nextLine>
    (if nextLine = null then 0 else countLine(nextLine) + countLinesFrom(in)) #
  BufferedReader(FileReader(file))  >in>
  countLinesFrom(in)  >count>
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

val inputList = take(numInputFiles, listFileNamesRecursively("../OrcTests/test_data/performance/distrib/holmes_test_data/"))

def testPayload() =
  map(repeatCountFilename, inputList)  >wordCountList>
  afold((+), wordCountList)


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
  ("Program", "wordcount-pure-orc.orc", "", "", ""),
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
Repetition ...: start.
Repetition ...: published ...
Repetition ...: finish.  Elapsed time ... µs
......
Repetitions' elapsed times output file written to ...
[[..., ...], ......]
-}
