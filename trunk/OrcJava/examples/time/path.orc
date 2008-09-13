{- Misra's shortest path simulation algorithm -}

{- 
Source and sink are node identifiers.
Succ is an expression which takes a node
identifiers and publishes the identifiers
of all of its immediate neighbors.
-} 
def path(source, sink, succ) =
  val timer = MakeTimer()
  def run(n,p) =
    def next([]) = rev(p)
    def next(ms) =
      each(ms) >(m,d)>
      timer(d) >>
      run(m,m:p)
    next(succ(n))
  let(run(source, [source]))

-- A small test graph
val source = 0
val sink = 3

def succ(0) = [(1,2), (2,6), (3,9)]
def succ(1) = [(3,7)]
def succ(2) = [(3,2)]
def succ(3) = []

-- Shortest path: [0,2,3]
path(source, sink, succ)