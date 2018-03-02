{- radixsort.orc - A parallel/distributed sort benchmark.
 - 
 - An implementation of LSB Radix sort. This is stable.
 -
 - The parallelism is within each sort phase for each digit.
 -}
 
-- FIXME: It may well be better to use bucketing instead of counting sort. 
-- The counting sort is extra complexity, but due to the merge that follows having buckets would probably be fine.

include "benchmark.inc"

import class BigSortData = "orc.test.item.scalabenchmarks.BigSortData"
--import class Arrays = "java.util.Arrays"
import class ArrayList = "java.util.ArrayList"
import class Collections = "java.util.Collections"

def supto(n, f) =
	def h(i) = Ift(i <: n) >> f(i) >> h(i+1)
	h(0) 

val base = 10
def getDigits(x) = 
	def h(i, p) if (x <: p) = i
	def h(i, p) = h(i + 1, p * base)
	h(0, 1) 
def getDigit(n, x) = (x / (base ** n).intValue()).intValue() % base

def List(n) = Collections.synchronizedList(ArrayList(n))
def ListFilled(n, v) = (
	val a = List(n)
	upto(n) >i> a.add(v) >> stop ;
	a
)

def mapList(f, in) = 
	val res = ListFilled(in.size(), null)
	upto(in.size()) >i> res.set(i, f(in.get(i))) >> stop ;
	res
	
def afoldList(f, in) if (in.size() <= 1) = in.get(0) 
def afoldList(f, in) =
	val split = (in.size() / 2).intValue()
	f(afoldList(f, in.subList(0, split)), afoldList(f, in.subList(split, in.size())))  

def mergeSorted(kf, inputs) =
  val indices = ListFilled(inputs.size(), 0)
  val output = List(1024)
  def minIndex() =
    def h(i, min, minInd) if (i = indices.size()) = minInd
    def h(i, None(), None()) =
      val y = indices.get(i) >j> Ift(j <: inputs.get(i).size()) >> kf(inputs.get(i).get(j))
      if (y >> true ; false) then
        h(i+1, Some(y), Some(i))
      else
        h(i+1, None(), None())
    def h(i, Some(min), Some(minInd)) =
      val y = indices.get(i) >j> Ift(j <: inputs.get(i).size()) >> kf(inputs.get(i).get(j))
      if (y <: min ; false) then
        h(i+1, Some(y), Some(i))
      else
        h(i+1, Some(min), Some(minInd))
    h(0, None(), None())
  def takeMinValue() =
    val iO = minIndex()
    iO >Some(i)> (
      inputs.get(i).get(indices.get(i)) >x>
      indices.set(i, indices.get(i) + 1) >> 
      Some(x)) |
    iO >None()> None() 
  def merge() =
    val x = takeMinValue()
    x >None()> signal |
    x >Some(y)> output.add(y) >> merge()
  merge() >> output

def splitSortMerge(kf, sort, input) =
	val partitionSize = Floor(input.size() / nPartitions)
	val sortedPartitions = ListFilled(Ceil(input.size().doubleValue() / partitionSize), null)
	forBy(0, input.size(), partitionSize) >start> 
		sortedPartitions.set(start / partitionSize, 
			sort(input.subList(start, min(start + partitionSize, input.size())))) >> stop ;
  	mergeSorted(kf, sortedPartitions)

def countingSort(kf, maxK, data) =
	val counts = ListFilled(maxK, 0)
	val out = ListFilled(data.size(), null)
	
	def histogram(i) if (i <: data.size()) =
		val k = kf(data.get(i))
		counts.set(k, counts.get(k) + 1) >>
		histogram(i + 1)
	def prefixsum(total, i) if (i <: maxK) =
		total + counts.get(i) >nextTotal>
		counts.set(i, total) >>
		prefixsum(nextTotal, i + 1)
	def place(i) if (i <: data.size()) =
		val k = kf(data.get(i))
		out.set(counts.get(k), data.get(i)) >> 
		counts.set(k, counts.get(k) + 1) >>
		place(i + 1)
		
	histogram(0) ;
	prefixsum(0, 0) ;
	place(0) ;
	out
		
	
def radixsort(data) = 
	val maxV = afoldList(max, data)
	val lastDigit = getDigits(maxV)
	def h(data, digit) =
		if digit :> lastDigit then
			data
		else
			val s = splitSortMerge({ getDigit(digit, _) }, { countingSort({ getDigit(digit, _) }, base, _) }, data)
			h(s, digit + 1)
	h(data, 0) >r>
	--Println(r) >>
	r
	

def setup() = 
	val a = BigSortData.makeRandomArray(BigSortData.arraySize())
	val l = List(a.length?)
	upto(a.length?) >i> l.add(a(i)?) >> stop ;
	l
	
  
benchmarkSized("RadixSort-naive", BigSortData.arraySize(), setup, radixsort, BigSortData.check) 


{-
BENCHMARK
-}

