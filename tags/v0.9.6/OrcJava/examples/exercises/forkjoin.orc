{--
Write a function which, given a list of sites, calls
each site in the list in parallel and publishes a
signal when all site calls have returned.
--}

def forkjoin([]) = signal
def forkjoin(f:rest) = (f(), forkjoin(rest)) >> signal

def example() = println("called")
forkjoin([example, example, example, example])

{-
OUTPUT:
called
called
called
called
signal
-}