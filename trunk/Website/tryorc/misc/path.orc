{-
Shortest path algorithm from:

Simulation using Orchestration
David Kitchin, Evan Powell, and Jayadev Misra
In Prcoeedings of AMAST, 2008
-}

{- 
Source and sink are node identifiers.
Cell is a mapping from from node identifiers 
to initially-empty write-once cells.
Succ is a function which takes a node
identifier and publishes the identifiers
of all of its immediate neighbors.
-} 
def path(source, sink, cell, succ) =
  def run(n,p) =
    cell(n).write(p) >>
    succ(n) >(m,d)>
    Ltimer(d) >>
    run(m,m:p)
  run(source, [source])
  ; reverse(cell(sink).read())

-- A small test graph
val source = 0
val sink = 3

val cell0 = Cell()
val cell1 = Cell()
val cell2 = Cell()
val cell3 = Cell()

def cell(0) = cell0
def cell(1) = cell1
def cell(2) = cell2
def cell(3) = cell3

def succ(0) = (1,2) | (2,6) | (3,9)
def succ(1) = (3,7)
def succ(2) = (3,2)
def succ(3) = stop

-- Shortest path: [0,2,3]
path(source, sink, cell, succ)
