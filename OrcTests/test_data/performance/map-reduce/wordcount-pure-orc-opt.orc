{- wordcount-pure-orc.orc -- Count words in text files, Pure Orc variant -}

{- This performance test counts words in a set of plain text files.  The
 - wordcount-input-data directory is walked, and a list of files is created.
 - The {{{repeatCountFilename}}} def is applied to each file in the list.
 - This def uses the {{{countFile}}} def {{{repeatRead}}} times to count the
 - the number of words in the given file.  A list of word counts (times
 - {{{repeatRead}}}) for each file is the result.  These counts are summed
 - using Orc's associative fold library function (afold).
 -}
 
-- This version of this benchmark uses Sequentialize() to eliminate excess concurrency.

include "wordcount.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"
import class BreakIterator = "java.text.BreakIterator"
import class Character = "java.lang.Character"

def containsAlphabetic(s, startPos, endPos) = SinglePublication() >> Sequentialize() >> startPos >> -- Inferable
  Character.isAlphabetic(s.codePointAt(startPos)) || (if startPos+1 <: endPos then containsAlphabetic(s, startPos+1, endPos) else false)

  def wordCount'(line, startPos, wb, accumCount) = SinglePublication() >> Sequentialize() >> accumCount >> -- Inferable (recursion)
    wb.next()  >endPos>
    (if endPos <: 0 then accumCount else (if containsAlphabetic(line, startPos, endPos) then wordCount'(line, endPos, wb, accumCount + 1) else wordCount'(line, endPos, wb, accumCount))) #

-- Lines: 10 (1)
def countLine(line) = SinglePublication() >> 
  line >> (
  wordCount' >>
  Sequentialize() >> ( -- Inferable
  BreakIterator.getWordInstance() >wb>
  wb.setText(line)  >>
  wordCount'(line, 0, wb, 0)
  )
  )

def countLinesFrom(in, accumCount) = SinglePublication() >> accumCount >>
  (in.readLine() ; null)  >nextLine> Sequentialize() >> -- Inferable (recursion)
  (if nextLine = null then accumCount else countLinesFrom(in, accumCount + countLine(nextLine)))

-- Lines: 9 (1)
def countFile(file) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  Sequentialize() >> -- Inferable
  BufferedReader(FileReader(file))  >in>
  countLinesFrom(in, 0)  >count>
  in.close()  >>
  count

def sumN(n, f) = if (n :> 0) then f() + sumN(n-1, f) else 0

-- Lines: 5
def repeatCountFilename(filename) =
  import class File = "java.io.File"
  File(filename)  >file>
  checkReadableFile(file)  >>
  sumN(repeatRead, { countFile(file) })


{--------
 - Test Procedure
 --------}

def setUpTest() =
  createTestDataFiles()

def setUpTestRep() =
  signal

-- Lines: 3
def runTestRep(inputList) =
  map(repeatCountFilename, inputList)  >wordCountList>
  afold((+), wordCountList)

def tearDownTestRep() =
  signal

def tearDownTest() =
  -- deleteTestDataFiles()
  signal

val inputList = (setUpTest() >> stop; signal) >> take(numInputFiles, listFileNamesRecursively(inputDataDirPath))
--val _ = Println(inputList)
 
benchmarkSized("wordcount-opt", numInputFiles * repeatRead, { setUpTestRep() >> inputList }, runTestRep, { tearDownTestRep() >> check(_) }) >> stop ;

tearDownTest()
