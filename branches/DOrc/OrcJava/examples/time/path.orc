{- Misra's shortest path simulation algorithm -}

{- 
	source and sink are node identifiers 
	
	cell is a mapping from node identifiers 
	  to initially empty write-once cells
	  
	succ is an expression which takes a node 
	  identifiers and publishes the identifiers 
	  of all of its immediate neighbors
-} 

def path(source, sink, cell, succ) =
   
    val timer = MakeTimer()
 
	def run(n) = cell(n).read() >p> 
	             succ(n) >(m,d)> 
	               timer(d) >> 
	               cell(m).write(m:p) >>
	               run(m)  
	                 	
	rev(cell(sink).read()) 
	  << cell(source).write([source])
	  << run(source)


{- A small test graph, defined procedurally -}

val cell0 = Cell()
val cell1 = Cell()
val cell2 = Cell()
val cell3 = Cell()

def cell(0) = cell0
def cell(1) = cell1
def cell(2) = cell2
def cell(3) = cell3

val source = 0
val sink = 3

def succ(0) = (1,2) | (2,6) | (3,9)
def succ(1) = (3,7)
def succ(2) = (3,2)
def succ(3) = null

{- Shortest path: [0,2,3] -}
path(source, sink, cell, succ)


 



