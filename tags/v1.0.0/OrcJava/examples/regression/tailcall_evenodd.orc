{-
Make sure tail calls are optimized for mutually recursive functions.
-}

def even(Integer) :: Boolean
def odd(Integer) :: Boolean
def even(n) = 
  if (n > 0) then odd(n-1)
  else if (n < 0) then odd(n+1)
  else true
def odd(n) = 
  if (n > 0) then even(n-1)
  else if (n < 0) then even(n+1)
  else false

val n = 2 ** 14 :!: Integer

even(n) | odd(1 - n)

{-
OUTPUT:
true
true
-}
  