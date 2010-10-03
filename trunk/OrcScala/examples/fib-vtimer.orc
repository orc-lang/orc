{-
author: amshali

Computes the fibonacci of a number using the virtual time
-}
def fibtik(0) = Vtimer(0)
def fibtik(1) = Vtimer(1)
def fibtik(n) = (fibtik(n-1) , fibtik(n-2)) >> signal

def fib(n) = fibtik(n) >> Vclock()

fib(10)

{-
OUTPUT:
55
-}
