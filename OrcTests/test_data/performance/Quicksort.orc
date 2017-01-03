{- Quicksort.orc -- Orc program Quicksort
 - 
 - Created by amp on Jul 20, 2013 12:29:59 PM
 -}

include "timeIt.inc"

def quicksort(a) =
  def part(p, s, t) =
    def lr(i) = if i <: t && a(i)? <= p then lr(i+1) else i 
    def rl(i) = if a(i)? :> p then rl(i-1) else i #
    (lr(s), rl(t)) >(s', t')>
    ( Ift(s' + 1 <: t') >> swap(a(s'),a(t')) >> part(p,s'+1,t'-1)  
    | Ift(s' + 1 = t') >> swap(a(s'),a(t')) >> s'
    | Ift(s' + 1 :> t') >> t'
    )
  def sort(s, t) =
    if s >= t then signal
    else part(a(s)?, s+1, t) >m>
         swap(a(m), a(s)) >>
         (sort(s, m-1), sort(m+1, t)) >>
         signal
  sort(0, a.length?-1) 

def makeRandomArray(n) =
  val a = Array(n) #
  (upto(n) >x> a(x) := Random(n) >> stop) ;
  a

def runTest() =
  timeIt({ makeRandomArray(1000) }) >a>
  --Println(arrayToList(a)) >>
  timeIt({ quicksort(a) })
  --Println(arrayToList(a))
  
runTest() 

{-
BENCHMARK
-}
