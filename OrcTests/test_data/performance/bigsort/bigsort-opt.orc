{- bigsort.orc -- A parallel/distributed sort benchmark.
 - 
 -}

import class BigSortData = "orc.test.item.scalabenchmarks.BigSortData"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

include "benchmark.inc"

-- Lines: 4
def orderedAnd(true, b) = b()
def orderedAnd(false, b) = false
def orderedOr(true, b) = true
def orderedOr(false, b) = b()

-- Lines: 13
class ArraySlice {
  val array
  val start
  val length
  def apply(i) = Sequentialize() >> array(i+start) -- Inferable
  val end
  def toList() = Sequentialize() >> ( -- Inferable (recursion)
    def walk(0,acc) = acc
    def walk(i,acc) = walk(i-1, this(i-1)? : acc)
    walk(length, [])
    )
  def toString() =
    "ArraySlice(" + start + " + " + toList() + ")" 
}
def ArraySlice(a, s, l) = a >a'> s >s'> l >l'> s + l >e'> Sequentialize() >> new ArraySlice { val array = a' # val start = s' # val length = l' # val end = e' }

    def lr(a, p, i, t) = SinglePublication() >> Sequentialize() >> a >> p >> i >> t >> 
        (if ~(i <: t && a(i)? <= p) then i else lr(a, p, i+1, t)) 
    def rl(a, p, i) = SinglePublication() >> Sequentialize() >> a >> p >> i >> 
        (if ~(a(i)? :> p) then i else rl(a, p, i-1)) #

  def part(a, p, s, t) = SinglePublication() >> Sequentialize() >> a >> p >> s >> t >>
    (lr(a, p, s, t), rl(a, p, t)) >(s', t')>
    ( Ift(s' + 1 = t') >> swap(a(s'),a(t')) >> s'
    | Ift(s' + 1 :> t') >> t'
    | Ift(s' + 1 <: t') >> swap(a(s'),a(t')) >> part(a, p,s'+1,t'-1)  
    )
  def sort(a, s, t) = SinglePublication() >> Sequentialize() >> a >> s >> t >> (
    if s >= t then signal
    else part(a, a(s)?, s+1, t) >m>
         swap(a(m), a(s)) >>
         sort(a, s, m-1) >>
         sort(a, m+1, t) >>
         signal
         )

-- Lines: 16
def quicksort(slice) = SinglePublication() >> Sequentialize() >> slice >> ( -- Inferable
  val a = slice.array
  sort(a, slice.start, slice.end - 1) >> slice
  )

-- Lines: 15
def mergeSorted(a :: ArraySlice, b :: ArraySlice) =
	SinglePublication() >> Sequentialize() >> a >> b >> (
    val outputLen = a.length + b.length
    val output = Array(outputLen)
    
    def h(aI, bI, oI) if (oI >= outputLen) = ArraySlice(output, 0, outputLen)
    def h(aI, bI, oI) =
      if orderedAnd(aI <: a.length, lambda() = orderedOr(bI >= b.length, lambda() = a(aI)? <: b(bI)?)) then
        output(oI) := a(aI)? >>
        h(aI+1, bI, oI+1)
      else if bI <: b.length then
        output(oI) := b(bI)? >>
        h(aI, bI+1, oI+1)
      else
        Error("IPE")
    h(0, 0, 0)
	)

-- Lines: 7
def splitSortMerge(input, sort) =
  val partitionSize = Floor(input.length? / nPartitions)
  val sortedPartitions = collect({
    forBy(0, input.length?, partitionSize) >start> (
      val sorted = sort(ArraySlice(input, start, min(partitionSize, input.length? - start)))
      sorted
    )
  })
  cfold(mergeSorted, sortedPartitions).array

def setup() = BigSortData.makeRandomArray(BigSortData.arraySize())
  
benchmarkSized("BigSort-opt", BigSortData.arraySize() * Log(BigSortData.arraySize()), setup, { splitSortMerge(_, quicksort) }, BigSortData.check)


{-
BENCHMARK
-}
