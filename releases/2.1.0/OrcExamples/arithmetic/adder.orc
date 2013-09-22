{- adder.orc -- Orc program: Simulate a hardware adder
 - 
 - Created by Amin Shali on Wednesday, June 09 2010
 -}

def sum(lambda(Integer)::Integer, Channel[Integer], Integer) :: Integer
def sum(a, b, n) =
	  def aux(Integer, Integer) :: Integer
  def aux(l, h) =
    if (l = h) then (a(l) >x> b.put(x) >> x)
      else aux(l, Floor((l+h)/2)) + aux(Floor((l+h)/2)+1, h)
  aux(0, n-1)

def seq_sum(Channel[Integer], Integer, Integer) :: Integer
def seq_sum(b, n, s) =
	  if n = 0 then s
	  else seq_sum(b, n-1, s+b.get())

val b = Channel[Integer]()
val N = 100

val a = Table(N, lambda(_ :: Integer) :: Integer = Random(10))

s1-s2 <(s1, s2)< (sum(a, b, N), seq_sum(b, N, 0))

{-
OUTPUT:
0
-}
