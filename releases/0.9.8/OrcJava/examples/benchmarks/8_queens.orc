{- Kitchin's 8-queens -}

{- The position of a queen on the chessboard is a coordinate pair -}
type Queen = (Integer,Integer)

def check(Queen,Queen) :: Top
def check((a,b),(x,y)) = if(a /= x) >> if(b /= y) >> if(a - b /= x - y) >> if(a + b /= x + y)

def addqueen(Queen, List[Queen]) :: List[Queen]
def addqueen(r, []) = [r]
def addqueen(r, q:qs) = check(r,q) >> q:(addqueen(r,qs))

def queens(Integer) :: List[Queen]
def queens(N) =
  def extend(List[Queen], Integer) :: List[Queen]
  def extend(x,0) = x
  def extend(x,n) = extend(x,n-1) >y> upto(N) >j> addqueen((n,j), y)
  extend([],N)

val clock = Clock()
collect(defer(queens, 6)) >x>
println("Time elapsed: " + clock() + "ms") >>
each(x)
