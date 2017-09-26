{- holmes-map-reduce.orc -- A d-Orc map-reduce -}

{- This performance test counts words in the 12 data files adventure-*.txt.
 - Each file is counted {{{repeatRead}}} times.  The file is read line-by-
 - line in to an Orc list, then each element (line) of the list is word
 - counted, which results in, for each file, a list (per-iteration) of
 - lists of per-line word counts. The multiple iterations of one file are
 - concatenated, and the resulting list is combined by folding the lists
 - with the + operator.  Then the resulting per-file word counts (times
 - {{{repeatRead}}}) are reduced, again by folding with the + operator.
 - The folds use Orc's associative fold library function (afold).
 -}

{- Number of times to re-read and word count each file. -}
{- Update the OUTPUT annotation when changing this. -}
val repeatRead = 1

def checkReadableFile(file) =
  import class JavaSys = "java.lang.System"
  if file.canRead() then signal else Error("Cannot read file: "+file+" in dir "+JavaSys.getProperty("user.dir")) >> stop

def readFile(file) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  BufferedReader(FileReader(file))  >in>
  (  def appendLinesFromIn(lines) =
      (in.readLine() ; null)  >nextLine>
      ( if nextLine = null
        then
          lines
        else
          appendLinesFromIn(nextLine:lines)
      )
    appendLinesFromIn([])
  )  >ss>
  in.close()  >>
  reverse(ss)

def wordCount(line) =
  import class BreakIterator = "java.text.BreakIterator"
  import class Character = "java.lang.Character"
  def wordCount'(wb, line) =
    def containsAlphabetic(s, startPos, endPos) =
      Character.isAlphabetic(s.codePointAt(startPos)) || (if startPos+1 <: endPos then containsAlphabetic(s, startPos+1, endPos) else false)
    def wordCount''(startPos) =
      wb.next()  >endPos>
      ( if endPos >= 0
        then
          (if containsAlphabetic(line, startPos, endPos) then 1 else 0) + wordCount''(endPos)
        else
          0
      ) #
    wb.setText(line)  >>
    wordCount''(0)
  BreakIterator.getWordInstance() >wb>
  wordCount'(wb, line)

def mapOperation(filename) =
  -- Run n copies of f to build a list.
  def loop(0, f) = []
  def loop(1, f) = [f()]
  def loop(n, f) = {| f() |} : loop(n-1, f) 
  
  import class File = "java.io.File"
  File(filename)  >f>
  checkReadableFile(f)  >>
  loop(repeatRead, 
    {
      readFile(f)  >lines>
      map(wordCount, lines)
    }
  )

def combineOperation(xs) = afold((+), concat(xs))

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
104484
-}