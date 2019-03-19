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

{- Number of times to re-read and word count each file. -}
{- Update the OUTPUT annotation when changing this. -}
val repeatRead = 10

def checkReadableFile(file) =
  import class Files = "java.nio.file.Files"
  import class JavaSys = "java.lang.System"
  if Files.isReadable(file) then signal else Error("Cannot read file: "+file+" in dir "+JavaSys.getProperty("user.dir")) >> stop

def countFile(file) =
  import class Files = "java.nio.file.Files"
  import class WordCount = "orc.test.item.distrib.WordCount"
  Files.newBufferedReader(file)  >in>
  WordCount.countReader(in)  >counts>
  in.close()  >>
  counts

def mapOperation(filename) =
  -- Run n copies of f to build a list.
  def loop(0, f) = []
  def loop(1, f) = [f()]
  def loop(n, f) = {| f() |} : loop(n-1, f) 

  import class Paths = "java.nio.file.Paths"
  Paths.get(filename)  >f>
  checkReadableFile(f)  >>
  loop(repeatRead,
    { countFile(f) }
  )

def combineOperation(xs) = afold((+), xs)

def reduceOperation(x, y) = x + y

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

{- Adjust this to 104484 * repeatRead. -}
{-
OUTPUT:
1044840
-}
