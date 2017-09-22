{- holmes-map-reduce.orc -- A d-Orc map-reduce -}

{- This performance test counts words in the 12 data files adventure-*.txt.
 - Each file is counted {{{repeatRead}}} times.  The file is read line-by-
 - line in to an Orc list, then each element (line) of the list is word
 - counted, which results in, for each file, a list (per-iteration) of
 - lists of per-line word counts. The multiple iterations of one file are
 - concatenated, and the resulting list is combined by folding the lists
 - with the + operator.  Then the resulting per-file word counts*10 are
 - reduced, again by folding with the + operator.  The folds use Orc's
 - associative fold library function (afold).
 -}

{- Number of times to re-read and word count each file. -}
{- Update the OUTPUT annotation when changing this. -}
val repeatRead = 1

def readFile(fn) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  import class File = "java.io.File"
  BufferedReader(FileReader(File(fn)))  >in>
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
  collect(
    { signals(repeatRead) >>
      readFile(filename)  >lines>
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
