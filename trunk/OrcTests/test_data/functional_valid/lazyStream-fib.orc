{- lazyStream-fib.orc -- Lazy Fibonacci sequence; Test case for closures
 - 
 - Created by arthur.peters on Jan 19, 2013
 -}

type Step[A] = Empty() | Item(A, lambda() :: Step[A])

def cons[A](x :: A, s :: lambda() :: Step[A]) = lambda () :: Step[A] = Item(x, s)
val nil = lambda () = Empty()

def mapStream[A, B](lambda(A) :: B, lambda() :: Step[A]) :: lambda() :: Step[B]
def mapStep[A, B](lambda(A) :: B, Step[A]) :: Step[B]
def mapStream(f, s) = lambda () = mapStep(f, s())
def mapStep(f, Empty()) = Empty()
def mapStep(f, Item(x, t)) = Item(f(x), mapStream(f, t))

def zipStreams[A, B, C](lambda(A, B) :: C, lambda () :: Step[A], lambda () :: Step[B]) :: lambda () :: Step[C]
def zipSteps[A, B, C](lambda(A, B) :: C, Step[A], Step[B]) :: Step[C]
def zipStreams(f, s, t) = lambda () = zipSteps(f, s(), t())
def zipSteps(f, _, Empty()) = Empty()
def zipSteps(f, Empty(), _) = Empty()
def zipSteps(f, Item(x, s), Item(y, t)) = Item(f(x,y), zipStreams(f, s, t))

def addInt(x :: Integer, y :: Integer) :: Integer = x + y

def sumStreams(lambda () :: Step[Integer], lambda () :: Step[Integer]) :: lambda () :: Step[Integer]
def sumStreams(s, t) = zipStreams(addInt, s, t)

def fib() :: lambda () :: Step[Integer] = sumStreams(cons(0, fib), cons(0, cons(1, fib)))() :!: lambda () :: Step[Integer]

def takeStep[A](Integer, Step[A]) :: List[A]
def takeStream[A](Integer, lambda() :: Step[A]) :: List[A]
def takeStep(0, _) = []
def takeStep(n, Item(x, s)) = x : takeStream(n-1, s)
def takeStream(n, s) = takeStep(n, s())

takeStream(10, fib :!: lambda() :: Step[Integer])

{-
OUTPUT:PERMUTABLE:
[0, 1, 1, 2, 3, 5, 8, 13, 21, 34]
-}
