{-
author: Amin Shali
date: Wednesday, June 09 2010
-}
def sum(lambda(Integer)::Integer, Buffer[Integer], Integer) :: Integer
def sum(a, b, n) =
	def aux(Integer, Integer) :: Integer
  def aux(l, h) =
    if (l = h) then (a(l)? >x> b.put(x) >> x)
    else aux(l, floor((l+h)/2)) + aux(floor((l+h)/2)+1, h)
  aux(0, n-1)

def seq_sum(Buffer[Integer], Integer, Integer) :: Integer
def seq_sum(b, n, s) =
	if n = 0 then s
	else seq_sum(b, n-1, s+b.get())

val b = Buffer[Integer]()
val N = 100

val a = IArray(N, lambda(_ :: Integer) :: Integer = random(10))

s1-s2<(s1, s2)< (sum(a, b, N), seq_sum(b, N, 0))

{-
OUTPUT:
0
-}
