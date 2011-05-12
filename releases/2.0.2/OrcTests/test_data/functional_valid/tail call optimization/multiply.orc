{-
Make sure tail calls are optimized for nested functions.
-}

def tailmult(Integer, Integer) :: Integer
def tailmult(a,b) = 
  def tailm(Integer, Integer) :: Integer
  def tailm(0,acc) = acc
  def tailm(a,acc) = tailm(a-1,acc+b)
  tailm(a,0)

tailmult(2, 30003) | tailmult(20002, 4) 

{-
OUTPUT:PERMUTABLE:
60006
80008
-}

  