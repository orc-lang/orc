{-
Make sure tail calls are optimized in the presence of >>.
-}

def helpfib(Integer, (Integer, Integer)) :: Integer
def helpfib(0, (a,_)) = a
def helpfib(n, (a,b)) = (b,a+b) >p> helpfib(n-1, p)

def fib(Integer) :: Integer
def fib(n) = helpfib(n, (1,1))

upto(10) >i> fib(Floor(2 ** i))

{-
OUTPUT:PERMUTABLE:
1
2
5
34
1597
3524578
17167680177565
407305795904080553832073954
229265413057075367692743352179590077832064383222590237
72639767602615659833415769076743441675530755653534070653184546540063470576806357692953027861477736726533858
-}
