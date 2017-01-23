val x = Rwait(1000) >> 42
def f(y) = Println("f"+y) >> x+y

def g() = Rwait(500) >> f
def h(ff) = ff(5)

Rwait(750) >> Println("Mark") >> stop | g >> "g"

{-
OUTPUT:
Mark
"g"
-}
