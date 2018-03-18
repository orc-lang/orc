{- shortest-path.orc -- Orc program: Single source shortest path on random graph
 -
 - Created by jthywiss on Feb 22, 2018 4:03:37 PM
 -}

include "vertex.inc"


{--------
 - Single source shortest path
 --------}

def swapIf[A](ref :: Ref[A], sem :: Semaphore, newVal :: A, test :: lambda(A, A) :: Boolean) :: Boolean  =
  withLock(sem, {
    -- Swap if test is true, or if Ref is unbound
    val shouldSwap = Iff(test(ref.readD(), newVal)) >> false; true
    if shouldSwap then
      ref := newVal >>
      true
    else
      false
  })
  
def gt(i :: EdgeWeight, j :: EdgeWeight) :: Boolean = i :> j

def updatePathLenFor(v :: Vertex, newPathLen :: EdgeWeight) :: Bot =
  if swapIf(v.pathLen(), v.pathLenSemaphore(), newPathLen, gt) then
    each(v.outEdges()) >e> updatePathLenFor(vertices(e.tail())?, newPathLen + e.weight())
  else
    stop


{--------
 - Test Procedure
 --------}

def setUpTest() =
  randomGraph(numVertices, probEdge, 40)

def setUpTestRep() =
  resetGraph()

def runTestRep() :: Signal =
  -- start at startVertex with path len 0
  updatePathLenFor(vertices(0)?, 0) >> stop;
  signal

def tearDownTestRep() =
  dumpGraph()

def tearDownTest() =
  signal

executeTest()


{-
OUTPUT:
Test start. Setting up test.
Repetition ...: setting up.
Repetition ...: start run.
Repetition ...: published ...
Repetition ...: finish run.  Elapsed time ... µs, leader CPU time ... ms
Repetition ...: tearing down.
......
Repetitions' elapsed times output file written to ...
[[..., ...], ......]
Tearing down test.
Test finish.
-}
