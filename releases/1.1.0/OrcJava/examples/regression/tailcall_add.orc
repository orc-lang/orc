{-
Make sure tail calls are optimized.
-}

def tailadd(Integer, Integer) :: Integer
def tailadd(0,n) = n
def tailadd(a,b) = tailadd(a-1,b+1)

tailadd(80001, 10008)

{-
OUTPUT:
90009
-}

  