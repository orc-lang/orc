{- bigsort.orc -- A parallel/distributed sort benchmark.
 - 
 -}

include "benchmark.inc"

import class BigSortData = "orc.test.item.scalabenchmarks.BigSortData"

def debugPrintln(v) = signal

class ArraySlice {
  val array
  val start
  val length
  def apply(i) = array(i+start)
  val end = start + length
  def toList() = 
    def walk(0,acc) = acc
    def walk(i,acc) = walk(i-1, this(i-1)? : acc)
    walk(length, [])
  def toString() =
    "ArraySlice(" + start + " + " + toList() + ")" 
}
def ArraySlice(a, s, l) = new ArraySlice { val array = a # val start = s # val length = l }

def quicksort(slice) =
  --val _ = Println(slice.toString())
  val a = slice.array
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
  sort(slice.start, slice.end - 1) >> slice

def mergeSorted(inputs) =
  val indices = (
    val a = Array(inputs.length?)
    upto(a.length?) >i> a(i) := 0 >> stop ;
    a
  )
  val outputLen = sum(collect({ upto(inputs.length?) >i> inputs(i)?.length }))
  val output = Array(outputLen)
  val _ = debugPrintln(outputLen)
  def minIndex() =
    def h(i, min, minInd) if (i = indices.length?) = minInd
    def h(i, None(), None()) =
      val _ = debugPrintln((i, None()))
      val y = indices(i)? >j> Ift(j <: inputs(i)?.length) >> inputs(i)?(j)?
      if (y >> true ; false) then
        h(i+1, Some(y), Some(i))
      else
        h(i+1, None(), None())
    def h(i, Some(min), Some(minInd)) =
      val _ = debugPrintln(("h(i, Some(min), Some(minInd))", i, min, minInd))
      val y = indices(i)? >j> Ift(j <: inputs(i)?.length) >> inputs(i)?(j)?
      if (y <: min ; false) then
        h(i+1, Some(y), Some(i))
      else
        h(i+1, Some(min), Some(minInd))
    h(0, None(), None())
  def takeMinValue() =
    val iO = minIndex()
    iO >Some(i)> (
      val x = inputs(i)?(indices(i)?)?
      val _ = debugPrintln(("takeMinValue() Some", i, x))
      x >> 
      indices(i) := indices(i)? + 1 >> 
      Some(x)) |
    iO >None()> None() 
  def merge(i) =
    val x = takeMinValue()
    val _ = debugPrintln(("merge(i)", i, arrayToList(indices), arrayToList(output)))
    val _ = debugPrintln(("merge(i)", i, x))
    x >None()> signal |
    x >Some(y)> output(i) := y >> merge(i+1)
  merge(0) >> output

def splitSortMerge(input, sort) =
  val partitionSize = Floor(input.length? / nPartitions)
  val sortedPartitions = collect({
    forBy(0, input.length?, partitionSize) >start> ( 
      val sorted = sort(ArraySlice(input, start, min(partitionSize, input.length? - start)))
      debugPrintln(sorted.toString()) >> sorted
    )
  })
  mergeSorted(listToArray(sortedPartitions))

val arraySize = problemSizeScaledInt(1000)

def setup() = BigSortData.makeRandomArray(arraySize)
  
benchmarkSized("BigSort-naive", arraySize * Log(arraySize), setup, { splitSortMerge(_, quicksort) }, BigSortData.check) 


{-
BENCHMARK
-}
