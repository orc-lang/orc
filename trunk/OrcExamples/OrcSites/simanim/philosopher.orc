{- philosopher.orc -- Orc program: Animated Dining Philosophers
 -
 - Originally by Joseph Cooper,
 - minor modifications by Adrian Quark.
 -}

import class Canvas = "orc.lib.simanim.Philosopher"

val numberOfPhilosophers = 10

val canvas =
  Canvas(numberOfPhilosophers) >canvas>
  canvas.open() >>
  canvas

def makeFork(n) =
  Channel() >f>
  f.put(true) >>
  Print("Made fork ",n,"\n") >>
  f

def releaseAnimate(p,s,i) =
  if i = 0 then
    true
  else
    canvas.setFork(p,s,i) >>
    Rwait(50) >>
    releaseAnimate(p,s,i-1)
     
def grabAnimate(p,s,i) =
  if i = 0 then
    true
  else
    canvas.setFork(p,s,11-i) >>
    Rwait(50) >>
    grabAnimate(p,s,i-1)

def getForks(n,fl,fr) =
  Let(
    getFork(n,1,fl) >> Let(
      getFork(n,0,fr)
      ; releaseFork(n, 1, fl) >> stop
    )
    ; Rwait(Random(100)*10+1) >>
      getForks(n, fl, fr)
  )

def releaseFork(n,side,f) =
  releaseAnimate(n,side,10) >>
  f.put(true)
 
def getFork(n,side,f) =
  f.getD() >>
  grabAnimate(n,side,10)

def eatAnimate(p,i,speed) =
  if i = 0 then
    true
  else
    canvas.setEat(p,i) >>
    Rwait(speed) >>
    eatAnimate(p,i-1,speed)

def eat(n) = 
  eatAnimate(n,10,index([100,200,300,500],Random(4))) >>
  canvas.setEat(n,0)

def thinkAnimate(p,i) =
  (if i = 11 then 1 else i) >x>
  canvas.setThink(p, x) >>
  Rwait(100) >>
  thinkAnimate(p,x+1)

def think(n) =
  Let(
    thinkAnimate(n,1)
    | Rwait(index([500,1000,2000,10000],Random(4)))
  ) >>
  canvas.setThink(n,0)

def letGo(n,fl,fr) =
  Let(releaseFork(n, 1, fl), releaseFork(n, 0, fr))

def philosopher(n,fl,fr) =
  think(n) >>
  getForks(n,fl,fr) >>
  eat(n) >>
  letGo(n,fl,fr) >>
  philosopher(n,fl,fr)

def redraw() =
  canvas.redraw() >>
  Rwait(50) >>
  redraw()
  
def clock() =
  Rwait(1) >>
  Rwait(1) >>
  clock()

def philosophers(n,lf,rf) =
  if n = 1 then
    philosopher(n-1,lf,rf)
  else (
    val nf = makeFork(true)
    philosopher(n-1,nf,rf) | philosophers(n-1,lf,nf)
  )

def dining(n) =
  val lf = makeFork(true)
  philosophers(n,lf,lf)
  | clock()
  | redraw()

dining(numberOfPhilosophers)
