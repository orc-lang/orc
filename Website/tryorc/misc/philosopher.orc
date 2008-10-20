def shuffle(a,b) = if (random(2) = 1) then (a,b) else (b,a)

def take((a,b)) =  
  a.acquire() >> b.acquirenb() ;
  a.release() >> take(shuffle(a,b))
    
def drop(a,b) = (a.release(), b.release()) >> signal

def phil(a,b,name) =
  def thinking() = Rtimer(random(1000))
  def hungry() = take((a,b))
  def eating() = 
    println(name + " is eating.") >> 
    Rtimer(random(1000)) >> 
    println(name + " has finished eating.") >>
    drop(a,b)
  thinking() >> hungry() >> eating() >> phil(a,b,name)

def dining(n) =
  val forks = IArray(n, lambda(_) = Semaphore(1))
  def phils(0) = stop
  def phils(i) =
      phil(forks(i%n), forks(i-1), "Philosopher " + i)
    | phils(i-1)
  phils(n) 
  
let(
    dining(5)
  | Rtimer(10000)
) >> "HALTED"
