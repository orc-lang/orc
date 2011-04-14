{-
Dining Philosophers.

Based on the randomized algorithm given in:
Lehmann, D. J., Rabin M. O.  On the Advantages of Free Choice: A Symmetric and
Fully Distributed Solution to the Dining Philosophers Problem. Principles Of
Programming Languages 1981 (POPL'81), pages 133-138.
-}

-- Randomly swap two elements
def shuffle(a,b) = if (random(2) = 1) then (a,b) else (b,a)

-- Acquire two forks in the order given
def take((a,b)) =  
  a.acquire() >> b.acquirenb() ;
  a.release() >> take(shuffle(a,b))
    
-- Release two forks
def drop(a,b) = (a.release(), b.release()) >> signal

-- Start a philosopher process with forks a and b
def phil(a,b,name) =
  def thinking() = Rtimer(random(1000))
  def hungry() = take((a,b))
  def eating() = 
    println(name + " is eating.") >> 
    Rtimer(random(1000)) >> 
    println(name + " has finished eating.") >>
    drop(a,b)
  thinking() >> hungry() >> eating() >> phil(a,b,name)

-- Start n philosophers dining in a ring
def dining(n) =
  val forks = IArray(n, lambda(_) = Semaphore(1))
  def phils(0) = stop
  def phils(i) =
      phil(forks(i%n), forks(i-1), "Philosopher " + i)
    | phils(i-1)
  phils(n) 
  
-- Simulate 5 philosophers for 10 seconds before halting
let(
    dining(5)
  | Rtimer(10000)
) >> "HALTED"
