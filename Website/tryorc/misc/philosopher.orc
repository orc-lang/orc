{-
Dining Philosophers.
Originally by Joseph Cooper,
minor modifications by Adrian Quark.
-}

val numberOfPhilosophers = 10
-- ratio of real time to simulated time
-- 1 means 1 second = 1 logical step
val slowdown = 1
-- number of steps to simulate
val time = 1000

def makeFork(n) =
  Buffer() >f>
  f.put(true) >>
  f

-- attempt to get both forks; if both cannot be gotten, wait
-- some random amount of time and try again
def getForks(n,fl,fr) =
  let(
    getFork(n,"left",fl) >> let(
      getFork(n,"right",fr)
      ; releaseFork(n, "left", fl) >> stop
    )
    ; Ltimer(1+random(5)) >>
      getForks(n, fl, fr)
  )

def releaseFork(n,side,f) =
  println("Philosopher " + n + " releasing " + side + " fork") >>
  f.put(true)
 
def getFork(n,side,f) =
  f.getnb() >>
  println("Philosopher " + n + " grabbing " + side + " fork")

def eat(n) =
  println("Philosopher " + n + " eating") >>
  Ltimer(1+random(4)) >>
  println("Philosopher " + n + " full")

def think(n) =
  println("Philosopher " + n + " thinking") >>
  Ltimer(1+random(4)) >>
  println("Philosopher " + n + " hungry")

def releaseForks(n,fl,fr) =
  let(releaseFork(n, "left", fl), releaseFork(n, "right", fr))

def philosopher(n,fl,fr) =
  think(n) >>
  getForks(n,fl,fr) >>
  eat(n) >>
  releaseForks(n,fl,fr) >>
  philosopher(n,fl,fr)

-- synchronize simulation time with real time
-- for demo purposes
def clock(n) =
  Rtimer(slowdown*1000) >>
  Ltimer(1) >>
  println("CLOCK TIME: " + n) >>
  clock(n+1)

def philosophers(n,lf,rf) =
  if n = 1 then
    philosopher(n-1,lf,rf)
  else (
    val nf = makeFork(true)
    philosopher(n-1,nf,rf) | philosophers(n-1,lf,nf)
  )

def dining(n) =
  let(
    val lf = makeFork(true)
    philosophers(n,lf,lf)
    | clock(1)
    | Ltimer(time)
  )

dining(numberOfPhilosophers) >> "DONE"
