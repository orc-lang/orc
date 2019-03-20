{- wordcount.inc -- Orc include: Word count tests common defintions
 -
 - Created by jthywiss on Apr 5, 2018 13:37
 -}

include "benchmark.inc"

import class JavaSys = "java.lang.System"

{- Number of files to read. -}
val numInputFiles = 24 -- problemSizeScaledInt(1.2) -- Read(JavaSys.getProperty("orc.test.numInputFiles", "12"))

{- Number of times to re-read and word count each file. -}
val repeatRead = 1 -- Read(JavaSys.getProperty("orc.test.repeatRead", "1"))

val orcTestsPath = JavaSys.getProperty("orc.test.benchmark.datadir", "../OrcTests/")

val holmesDataDirPath = orcTestsPath + "test_data/functional_valid/distrib/holmes_test_data"

val inputDataDirPath = orcTestsPath + "test_data/performance/distrib/wordcount/wordcount-input-data/"

val targetFileSize = 17895697 {- bytes -} -- = 2 GiB / 120

val numCopiesInputFiles = 120 -- can be numInputFiles, if we delete and re-gen input files for every condition


{--------
 - I/O Utilities
 --------}

def checkReadableFile(file) =
  import class Files = "java.nio.file.Files"
  if Files.isReadable(file) then signal else Error("Cannot read file: "+file+" in dir "+JavaSys.getProperty("user.dir")) >> stop

def listFileNamesRecursively(dirPathName :: String) :: List[String] =
  import class Paths = "java.nio.file.Paths"
  import class WordCount = "orc.test.item.distrib.WordCount"
  WordCount.listFileNamesRecursively(Paths.get(dirPathName))  >pathnameArray>
  arrayToList(pathnameArray)

def createTestDataFiles() =
  import class WordCount = "orc.test.item.distrib.WordCount"
  WordCount.createTestFiles(holmesDataDirPath, inputDataDirPath, targetFileSize, numCopiesInputFiles)

def deleteTestDataFiles() =
  import class WordCount = "orc.test.item.distrib.WordCount"
  WordCount.deleteTestFiles(inputDataDirPath, numCopiesInputFiles)

def check(x) =
  import class WordCount = "orc.test.item.scalabenchmarks.WordCount"
  WordCount.check(x)
  