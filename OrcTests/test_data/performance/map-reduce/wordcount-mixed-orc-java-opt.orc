{- wordcount-mixed-orc-java.orc -- Count words in text files, Mixed Orc-Java variant -}

{- This performance test counts words in a set of plain text files.  The
 - wordcount-input-data directory is walked, and a list of files is created.
 - The {{{repeatCountFilename}}} def is applied to each file in the list.
 - This def uses our WordCount utility Java class {{{repeatRead}}} times to
 - count the number of words in the given file.  A list of word counts (times
 - {{{repeatRead}}}) for each file is the result.  These counts are summed
 - using Orc's associative fold library function (afold).
 -}

include "wordcount.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

def countFile(file) =
  import class Files = "java.nio.file.Files"
  import class WordCount = "orc.test.item.distrib.WordCount"
  Sequentialize() >> -- Inferable
  Files.newBufferedReader(file)  >in>
  WordCount.countReader(in)  >count>
  in.close()  >>
  count

def repeatCountFilename(filename) =
  import class Paths = "java.nio.file.Paths"
  def sumN(n, f) = if (n :> 0) then f() + sumN(n-1, f) else 0

  Paths.get(filename)  >file>
  checkReadableFile(file)  >>
  sumN(repeatRead, { countFile(file) })


{--------
 - Test Procedure
 --------}

def setUpTest() =
  createTestDataFiles()

def setUpTestRep() =
  signal

def runTestRep(inputList) =
  map(repeatCountFilename, inputList)  >wordCountList>
  afold((+), wordCountList)

def tearDownTestRep() =
  signal

def tearDownTest() =
  -- deleteTestDataFiles()
  signal

val inputList = (setUpTest() >> stop; signal) >> take(numInputFiles, listFileNamesRecursively(inputDataDirPath))
-- val _ = Println(inputList)
 
benchmarkSized("wordcount-java-opt", numInputFiles * repeatRead, { setUpTestRep() >> inputList }, runTestRep, { tearDownTestRep() >> check(_) }) >> stop ;

tearDownTest()
