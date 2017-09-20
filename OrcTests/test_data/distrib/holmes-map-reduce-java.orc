{- holmes-map-reduce-java.orc -- A d-Orc map-reduce -}

import site Location0PinnedTuple = "orc.run.porce.distrib.Location0PinnedTupleConstructor"
import site Location1PinnedTuple = "orc.run.porce.distrib.Location1PinnedTupleConstructor"
import site Location2PinnedTuple = "orc.run.porce.distrib.Location2PinnedTupleConstructor"
import site Location3PinnedTuple = "orc.run.porce.distrib.Location3PinnedTupleConstructor"
import site Location4PinnedTuple = "orc.run.porce.distrib.Location4PinnedTupleConstructor"
import site Location5PinnedTuple = "orc.run.porce.distrib.Location5PinnedTupleConstructor"
import site Location6PinnedTuple = "orc.run.porce.distrib.Location6PinnedTupleConstructor"
import site Location7PinnedTuple = "orc.run.porce.distrib.Location7PinnedTupleConstructor"
import site Location8PinnedTuple = "orc.run.porce.distrib.Location8PinnedTupleConstructor"
import site Location9PinnedTuple = "orc.run.porce.distrib.Location9PinnedTupleConstructor"
import site Location10PinnedTuple = "orc.run.porce.distrib.Location10PinnedTupleConstructor"
import site Location11PinnedTuple = "orc.run.porce.distrib.Location11PinnedTupleConstructor"
import site Location12PinnedTuple = "orc.run.porce.distrib.Location12PinnedTupleConstructor"
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

def readerForFile(fn) =
  import class BufferedReader = "java.io.BufferedReader"
  import class FileReader = "java.io.FileReader"
  BufferedReader(FileReader(fn))


def mapOperation(shard) =
  import class WordCount = "orc.test.item.distrib.WordCount"
  collect(
    { signals(10) >>
      each(shard(0)) >filename>
      readerForFile(filename)  >rdr>
      WordCount.countReader(rdr)
    }
  )

def combineOperation(xs) = afold((+), xs)

def reduceOperation(x, y) = x + y

"test_data/distrib/holmes_test_data/"  >dataDir>

[
  Location1PinnedTuple([dataDir + "adventure-1.txt"], 1),
  Location2PinnedTuple([dataDir + "adventure-2.txt"], 2),
  Location3PinnedTuple([dataDir + "adventure-3.txt"], 3),
  Location4PinnedTuple([dataDir + "adventure-4.txt"], 4),
  Location5PinnedTuple([dataDir + "adventure-5.txt"], 5),
  Location6PinnedTuple([dataDir + "adventure-6.txt"], 6),
  Location7PinnedTuple([dataDir + "adventure-7.txt"], 7),
  Location8PinnedTuple([dataDir + "adventure-8.txt"], 8),
  Location9PinnedTuple([dataDir + "adventure-9.txt"], 9),
  Location10PinnedTuple([dataDir + "adventure-10.txt"], 10),
  Location11PinnedTuple([dataDir + "adventure-11.txt"], 11),
  Location12PinnedTuple([dataDir + "adventure-12.txt"], 12)
] >inputList>


map(mapOperation, inputList)  >mappedList>
map(combineOperation, mappedList)  >combinedList>
afold(reduceOperation, combinedList)

{-
OUTPUT:
1044840
-}
