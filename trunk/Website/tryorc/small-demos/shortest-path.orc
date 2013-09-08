{- shortest-path.orc -- Orc program: Find shortest path in a directed graph
 - 
 - See:
 -   Kitchin, D., Powell, E., Misra, J.: Simulation Using Orchestration:
 -   Proceedings of AMAST 2008. LNCS 5140, 2-15. (2008)
 - 
 -}

{- 
Source and sink are node identifiers.
Cell is a mapping from from node identifiers
to initially-empty write-once cells.
Succ is a function which takes a node
identifier and publishes the identifiers
of all of its immediate neighbors.
-}

Vclock(IntegerTimeOrder) >> Vawait(0) >> (
type Node = Integer
type Distance = Number

def path(source :: Node,
		 sink :: Node,
		 cell :: lambda(Node) :: Cell[List[Node]],
		 succ :: lambda(Node) :: (Node,Distance)
		 ) :: List[Node] =
  def run(n :: Node, p :: List[Node]) :: Bot =
    cell(n).write(p) >>
    succ(n) >(m,d)>
    Vawait(d) >>
    run(m,m:p)
  run(source, [source])
  ; reverse(cell(sink).read())

-- A small test graph
val source = 0
val sink = 3

def mkcell() = Cell[List[Node]]()

val cell0 = mkcell()
val cell1 = mkcell()
val cell2 = mkcell()
val cell3 = mkcell()

def cell(Node) :: Cell[List[Node]]
def cell(0) = cell0
def cell(1) = cell1
def cell(2) = cell2
def cell(3) = cell3

def succ(Node) :: (Node,Distance)
def succ(0) = (1,2) | (2,6) | (3,9)
def succ(1) = (3,7)
def succ(2) = (3,2)
def succ(3) = stop

path(source, sink, cell, succ)
)

{-
OUTPUT:
[0, 2, 3]
-}
