{- Kitchin's 8-queens -}
def check((a,b),(x,y)) = if(a /= x) >> if(b /= y) >> if(a - b /= x - y) >> if(a + b /= x + y)

def addqueen(r, []) = [r]
def addqueen(r, q:qs) = check(r,q) >> q:(addqueen(r,qs))

def range(n) = if (n > 0) then ( n-1 | range(n-1) ) else stop

def queens(N) =
  def extend(x,0) = x
  def extend(x,n) = extend(x,n-1) >y> range(N) >j> addqueen((n,j), y)
  extend([],N)

val clock = Clock()
collect(defer(queens, 6)) >x> (
  each(x) | clock()
)