{- holmes-map-reduce.orc -- A d-Orc map-reduce -}

import site Location0PinnedTuple = "orc.run.distrib.Location0PinnedTupleConstructor"
import site Location1PinnedTuple = "orc.run.distrib.Location1PinnedTupleConstructor"
import site Location2PinnedTuple = "orc.run.distrib.Location2PinnedTupleConstructor"
import site Location3PinnedTuple = "orc.run.distrib.Location3PinnedTupleConstructor"
import site Location4PinnedTuple = "orc.run.distrib.Location4PinnedTupleConstructor"
import site Location5PinnedTuple = "orc.run.distrib.Location5PinnedTupleConstructor"
import site Location6PinnedTuple = "orc.run.distrib.Location6PinnedTupleConstructor"
import site Location7PinnedTuple = "orc.run.distrib.Location7PinnedTupleConstructor"
import site Location8PinnedTuple = "orc.run.distrib.Location8PinnedTupleConstructor"
import site Location9PinnedTuple = "orc.run.distrib.Location9PinnedTupleConstructor"
import site Location10PinnedTuple = "orc.run.distrib.Location10PinnedTupleConstructor"
import site Location11PinnedTuple = "orc.run.distrib.Location11PinnedTupleConstructor"
import site Location12PinnedTuple = "orc.run.distrib.Location12PinnedTupleConstructor"
{-
-- Dummies for local testing
def Location0PinnedTuple(x,y) = (x,y)
def Location1PinnedTuple(x,y) = (x,y)
def Location2PinnedTuple(x,y) = (x,y)
def Location3PinnedTuple(x,y) = (x,y)
def Location4PinnedTuple(x,y) = (x,y)
def Location5PinnedTuple(x,y) = (x,y)
def Location6PinnedTuple(x,y) = (x,y)
def Location7PinnedTuple(x,y) = (x,y)
def Location8PinnedTuple(x,y) = (x,y)
def Location9PinnedTuple(x,y) = (x,y)
def Location10PinnedTuple(x,y) = (x,y)
def Location11PinnedTuple(x,y) = (x,y)
def Location12PinnedTuple(x,y) = (x,y)
-}

def readFile(fn) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  BufferedReader(FileReader(fn))  >in>
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

def mapOperation(shard) =
  collect(
    { signals(10) >>
      each(shard(0)) >filename>
      readFile(filename)  >lines>
      map(wordCount, lines)
    }
  )

def combineOperation(xs) = afold((+), concat(xs))

def reduceOperation(x, y) = x + y

[
  Location1PinnedTuple(["holmes_test_data/adventure-1.txt"], 1),
  Location2PinnedTuple(["holmes_test_data/adventure-2.txt"], 2),
  Location3PinnedTuple(["holmes_test_data/adventure-3.txt"], 3),
  Location4PinnedTuple(["holmes_test_data/adventure-4.txt"], 4),
  Location5PinnedTuple(["holmes_test_data/adventure-5.txt"], 5),
  Location6PinnedTuple(["holmes_test_data/adventure-6.txt"], 6),
  Location7PinnedTuple(["holmes_test_data/adventure-7.txt"], 7),
  Location8PinnedTuple(["holmes_test_data/adventure-8.txt"], 8),
  Location9PinnedTuple(["holmes_test_data/adventure-9.txt"], 9),
  Location10PinnedTuple(["holmes_test_data/adventure-10.txt"], 10),
  Location11PinnedTuple(["holmes_test_data/adventure-11.txt"], 11),
  Location12PinnedTuple(["holmes_test_data/adventure-12.txt"], 12)
] >inputList>


map(mapOperation, inputList)  >mappedList>
map(combineOperation, mappedList)  >combinedList>
afold(reduceOperation, combinedList)

{-
OUTPUT:
1044840
-}
