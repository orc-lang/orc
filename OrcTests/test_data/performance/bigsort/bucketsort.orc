{- bucketsort.orc -- A parallel/distributed sort benchmark.
 -
 - A "bucket" sort, aka a recursive MSB radix sort. This is unstable.
 -
 - This produces a tree of parallel tasks each of which is sorting one bucket of 
 - it's parent. The leaf nodes use java.util.Collections.sort on "limit" items.
 - In addition, bucketing is done concurrently for all elements in the input
 - sequence (either the input data or a bucket).
 -}

include "benchmark.inc"

import class BigSortData = "orc.test.item.scalabenchmarks.BigSortData"
--import class Arrays = "java.util.Arrays"
import class ArrayList = "java.util.ArrayList"
import class Collections = "java.util.Collections"

val limit = 8

val base = 10
def getDigits(x) = 
	def h(i, p) if (x <: p) = i
	def h(i, p) = h(i + 1, p * base)
	h(0, 1) 
def getDigit(n, x) = (x / (base ** n).intValue()).intValue() % base

def List(n) = Collections.synchronizedList(ArrayList(n))

def mapList(f, in) = 
	val res = (
		val l = List(in.size())
		upto(in.size()) >> l.add(null) >> stop ;
		l
	)
	upto(in.size()) >i> res.set(i, f(in.get(i))) >> stop ;
	res
	
def afoldList(f, in) if (in.size() = 1) = in.get(0) 
def afoldList(f, in) =
	val split = (in.size() / 2).intValue()
	f(afoldList(f, in.subList(0, split)), afoldList(f, in.subList(split, in.size())))  

def bucketData(data, digit) =
	val buckets = (
		val l = List(base)
		upto(base) >> l.add(List(16)) >> stop ;
		l
	)
	upto(data.size()) >i> (
		val v = data.get(i)
		val d = getDigit(digit, v)
		buckets.get(d).add(v)
	) >> stop;
	--Println(digit + " " + buckets) >>
	buckets

def bucketsort(data) = 
	val maxV = afoldList(max, data)
	val firstDigit = getDigits(maxV)
	val _ = Println((maxV, firstDigit))
	def h(data, digit) =
		if data.size() <: limit || digit <: 0 then
			Collections.sort(data) >> data
		else
			afoldList(lambda(x, y) = x.addAll(y) >> x, mapList({ h(_, digit - 1) }, bucketData(data, digit)))
	h(data, firstDigit) >r>
	--Println(r) >>
	r
	

def setup() = 
	val a = BigSortData.makeRandomArray(BigSortData.arraySize())
	val l = List(a.length?)
	upto(a.length?) >i> l.add(a(i)?) >> stop ;
	l
	
  
benchmarkSized("BucketSort-naive", BigSortData.arraySize(), setup, bucketsort, BigSortData.check) 


{-
BENCHMARK
-}

