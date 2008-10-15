{-
include "prelude/op.inc"
site cat = orc.lib.str.Cat
site print = orc.lib.str.Print
site println = orc.lib.str.Println
site parseInt = orc.lib.str.AtoI
site parseBool = orc.lib.str.AtoB

site if = orc.runtime.sites.core.If

type Channel = orc.lib.types.Channel
type Tree a = Node(Tree a, Tree a, a) | Leaf() | orc.lib.types.Single

type T a b = Node(Tree, Tree)

type Pair a b = (a,b)

sig preorder(Tree a) :: [a]
def preorder(Leaf()) = []
def preorder(Node(l,r,n)) = append(preorder(l), append(preorder(r),[n]))

sig Metronome :: () -> Top
def Metronome() = ... 

def f(Number) :: Number
def f(n) = if(n > 1) >> n*f(n-1) | if(n <= 1) >> 1 

def g(Number, Number) :: Number
def g(a,b) = b+a

f :: Integer -> Integer -> Integer
f x y = x + y

f(3) | (4,5) >(a,b)> g(b,a)
-}

def shuffle(a,b) = if (random(2) = 1) then (a,b) else (b,a)

def take((a,b)) =  
  a.acquire() >> b.acquirenb() ;
  a.release() >> take(shuffle(a,b))
    
def drop(a,b) = (a.release(), b.release()) >> signal

def phil(a,b,name) =
  def thinking() = 
    if (urandom() < 0.9)
      then Rtimer(random(1000))
      else println(name + " is thinking forever.") >> stop
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
  def phils(i) = phil(forks(i%n), forks(i-1), "Philosopher " + i) | phils(i-1)
  phils(n) 
  
dining(5) ; println("Done.") >> stop


