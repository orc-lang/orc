{- dfs.orc -- Orc program: Perform a depth-first search of a graph

This is the depth first search algorithm for a connected undirected
graph. The graph structure is embedded in an immutable array conn; conn(i), 0<= i<
N, is the list of neighbors of i. The output of the algorithm is the
depth-first search tree, which is represented by array
parent: parent(i) is N if i is the root (node 0), and 0<= j< N, otherwise.

Since the graph is connected, conn(i) is never []. array parent is
mutable. An invariant of the algorithm is: parent(i) is negative if i
is not the root(node 0) nor has a parent, N if i is the root (node 0),
and j, 0<= j< N, if i has parent j.

Edge (i,j) is a tree edge if i is j's parent, i.e., parent(j) = i;
(i,j) is backward if j is an ancestor, possibly parent, of i,
i.e., parent(j) =/ i
-}

val N = 6
val conn = Array[List[Integer]](N)
val parent = fillArray(Array[Integer](N), lambda(_ :: Integer) = -1)

def dfs(Integer) :: Signal
def dfs(i) =
  def scan(List[Integer]) :: Signal
  def scan([]) = signal
  def scan(y:ys) =
    if parent(y)? <: 0 then
      ( parent(y) := i >> dfs(y) >> scan(ys) )
    else
      scan(ys)
  scan(conn(i)?)

-- Goal expression. First specify the graph structure.
#
( conn(0) := [1,2,3,4]
, conn(1) := [0,5]
, conn(2) := [0,4]
, conn(3) := [0,5]
, conn(4) := [0,2]
, conn(5) := [1,3]
)
>>
parent(0) := N >> dfs(0) >> upto(N) >i> (i,parent(i)?)

-- Note that this test may occasionally spontaneously fail due to nondeterminism,
-- since some pairs might occur in a different order.
-- This is just a weakness of the test harness.

{-
OUTPUT:PERMUTABLE:
(0, 6)
(1, 0)
(2, 0)
(3, 5)
(4, 2)
(5, 1)
-}
