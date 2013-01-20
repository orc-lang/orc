type Step[A] = Empty() | Item(A, lambda() :: Step[A])

def cons[A](x :: A, s :: lambda() :: Step[A]) = lambda () :: Step[A] = Item(x, s)
val nil = lambda () = Empty()

def mapStream[A, B](f :: lambda(A) :: B, s :: lambda() :: Step[A]) :: lambda() :: Step[B] = lambda () :: Step[B] = mapStep(s())
def mapStep(f, Empty()) = Empty()
def mapStep(f, Item(x, t)) = Item(f(x), mapStream(f, t))

def zipStreams(f, s, t) = lambda () = zipSteps(f, s(), t())
def zipSteps(f, _, Empty()) = Empty()
def zipSteps(f, Empty(), _) = Empty()
def zipSteps(f, Item(x, s), Item(y, t)) = Item(f(x,y), zipStreams(f, s, t))

def sumStreams(s, t) = zipStreams((+), s, t)

def fib() = sumStreams(cons(0, fib), cons(0, cons(1, fib)))()

def takeStep(0, _) = []
def takeStep(n, Item(x, s)) = x : takeStream(n-1, s)
def takeStream(n, s) = takeStep(n, s())

takeStream(10, fib)

{-
OUTPUT:PERMUTABLE:
[0, 1, 1, 2, 3, 5, 8, 13, 21, 34]
-}
