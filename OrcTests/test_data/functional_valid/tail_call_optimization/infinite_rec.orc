{-
Make sure tail calls are actually optimized in the simple case and we are not just getting lucky.
This will put a really vast number of calls on the stack without the runtime having any chance
to trampoline at a site call or anything.
-}

-- f is tightly recursive with a modified P
def f(0) = 0
def f(x) = f(x-1) >> x

-- g is tightly recursive with no change to P
def g(0) = 0
def g(x) = g(x-1)

-- h is tightly recursive and has work do after the the recursive call (but not in P)
def h(0) = 0
def h(x) = val v = h(x-1) # x

f(50000) |
g(50000) |
h(50000)

{-
OUTPUT:PERMUTABLE
50000
0
50000
-}
