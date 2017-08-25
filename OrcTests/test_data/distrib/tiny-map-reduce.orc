{- tiny-map-reduce.orc -- A toy d-Orc map-reduce -}

import site Location0PinnedTuple = "orc.run.porce.distrib.Location0PinnedTupleConstructor"
import site Location1PinnedTuple = "orc.run.porce.distrib.Location1PinnedTupleConstructor"
import site Location2PinnedTuple = "orc.run.porce.distrib.Location2PinnedTupleConstructor"


Location1PinnedTuple([1, 2, 3, 4, 5], signal)  >shard1>
Location2PinnedTuple([10, 20, 30, 40, 50], signal)  >shard2>
[shard1, shard2]  >theList>

(
def mappedOperation(shard) = map(lambda(x) = x * 10, shard(0)) 

def reducingOperation(xs, ys) = afold((+), xs) + afold((+), ys)

map(mappedOperation, theList)  >mappedList>
afold(reducingOperation, mappedList)

)

{-
xOUTPUT:
1650
-}
