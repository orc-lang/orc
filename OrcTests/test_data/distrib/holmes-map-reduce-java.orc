{- holmes-map-reduce-java.orc -- A d-Orc map-reduce -}

{- This performance test counts words in the 12 data files adventure-*.txt.
 - Each file is counted 10 times.  The file is processed by this test's
 - WordCount Java class, which results in, for each file, a list (per-
 - iteration) of file word counts.  The multiple iterations of one file
 - are combined by folding the lists with the + operator.  Then the resulting
 - per-file word counts*10 are reduced, again by folding with the + operator.
 - The folds use Orc's associative fold library function (afold).
 -}

def readerForFile(fn) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  import class File = "java.io.File"
  BufferedReader(FileReader(File(fn)))


def mapOperation(filename) =
  import class WordCount = "orc.test.item.distrib.WordCount"
  collect(
    { signals(10) >>
      readerForFile(filename)  >rdr>
      WordCount.countReader(rdr)
    }
  )

def combineOperation(xs) = afold((+), xs)

def reduceOperation(x, y) = x + y

"../OrcTests/test_data/distrib/holmes_test_data/"  >dataDir>

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

{-
OUTPUT:
1044840
-}
