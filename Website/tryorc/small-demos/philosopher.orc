{-
Dining Philosophers.

Based on the randomized algorithm given in:
Lehmann, D. J., Rabin M. O.  On the Advantages of Free Choice: A Symmetric and
Fully Distributed Solution to the Dining Philosophers Problem. Principles Of
Programming Languages 1981 (POPL'81), pages 133-138.
-}

-- Randomly swap two elements
def shuffle(a,b) = if (Random(2) = 1) then (a,b) else (b,a)

-- Acquire two forks in the order given
def take((a,b)) =
  a.acquire() >> b.acquireD() ;
  a.release() >> take(shuffle(a,b))

-- Release two forks
def drop(a,b) = (a.release(), b.release()) >> signal

-- Start a philosopher process with forks a and b
def phil(a,b,name) =
  def thinking() = Rwait(Random(1000))
  def hungry() = take((a,b))
  def eating() =
    Println(name + " is eating.") >>
    Rwait(Random(1000)) >>
    Println(name + " has finished eating.") >>
    drop(a,b)
  thinking() >> hungry() >> eating() >> phil(a,b,name)

-- Start n philosophers dining in a ring
def dining(n) =
  val forks = Table(n, lambda(_) = Semaphore(1))
  def phils(0) = stop
  def phils(i) =
      phil(forks(i%n), forks(i-1), "Philosopher " + i)
    | phils(i-1)
  phils(n)

-- Simulate 5 philosophers for 10 seconds before halting
Let( dining(5) | Rwait(10000) ) >>
Println("Simulation stopped.") >>
stop
