val x = Rwait(1000) >> 42
def f(y) = Println("f"+y) >> x+y

val g = Rwait(500) >> f
def h(ff) = ff(5)

Rwait(750) >> Println("Mark") >> stop | (g(10), h(f))

{-
OUTPUT:
f5
Mark
f10
(52, 47)
-}
