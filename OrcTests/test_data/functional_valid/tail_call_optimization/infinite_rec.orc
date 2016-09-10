{-
Make sure tail calls are actually optimized in the simple case and we are not just getting lucky.
This will put a really vast number of calls on the stack without the runtime having any chance
to trampoline at a site call or anything.
-}

def f(0) = 0
def f(x) = f(x-1) >> x
def g(0) = signal
def g(x) = g(x-1)

f(50000) |
g(50000)

{-
OUTPUT:PERMUTABLE:
50000
signal
-}
