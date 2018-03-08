{- fp-growth.orc -- Frequent Itemset Mining using FP-Growth
 -
 - Created by amp on Mar 2, 2018 5:09:15 PM
 -}

include "benchmark.inc"
 
import class LongAdder = "java.util.concurrent.atomic.LongAdder"

import class JList = "java.util.List"
import class JArrayList = "java.util.ArrayList"
import class JCollections = "java.util.Collections"

{--
@def foldPublications[A](lambda () :: A, lambda (A, A) :: A) :: A
Reduce all publications of src using f. Because publications are unordered, f must be commutative and 
associative for the result to be deterministic.

This function opportunistically reduces independent pairs of publications in parallel, so publications
are reduced as soon as possible.

@implementation
--}
def foldPublications[A](lambda () :: A, lambda (A, A) :: A) :: A
def foldPublications(src, f) =
	-- FIXME: This implementation's performance relies on the fact that if a channel had a blocked get then a put will unblock that get immediately guarenteeing that a get call that occures after the put cannot steal the item. 
	--   Many implementations will have this property, but notably distributed implementations probably will not since the local channel proxy may not know about a remove get.
	--   This implementation will still be CORRECT with a more relaxed channel, but it can degrade to sequential performance if every values in ch is captured immediately by the reduction() immediately after the put.
	val ch = Channel()
	val excludeOneReduction = Semaphore(1)
	
	{-
	Perform a single reduction on values in ch if needed.
	
	One value should be left in ch as the result, so the first call to
	reduction() is ignored.
	-}
	def reduction() =
		excludeOneReduction.acquireD() ; 
		ch.get() >x>
		ch.get() >y>
		ch.put(f(x,y))

	src() >x>
		ch.put(x) >> reduction() >> stop ; ch.getD()
	 

def ArrayList_apply() = new ArrayList {
	val underlying = JCollections.synchronizedList(JArrayList())
}

def ArrayList_withSizeHint(n :: Integer) = new ArrayList {
	val underlying = JCollections.synchronizedList(JArrayList(n))
}

def ArrayList_fill(n :: Integer, f :: lambda() :: Top) = (
	val a = ArrayList_withSizeHint(n)
	signals(n) >> a.add(f()) >> stop ;
	a
)

def ArrayList_wrap(s) = new ArrayList {
	val underlying = s 
} 

class ArrayList {
	val underlying :: JList
	
	-- Atomic operations
	
	val get = underlying.get
	val set = underlying.set
	val add = underlying.add
	val contains = underlying.contains
	val size = underlying.size
	val clear = underlying.clear
	
	def subList(i, j) =
		ArrayList_wrap(underlying.subList(i, j))
		
	def head() = get(0)
	
	-- Non-atomic operations
	
	def map(f) =
		val res = ArrayList_fill(size(), { null })
		upto(size()) >i> res.set(i, f(get(i))) >> stop ;
		res
		
	def afold(f) if (size() = 0) = stop
	def afold(f) if (size() = 1) = get(0)
	def afold(f) =
		val split = (size() / 2).intValue()
		f(subList(0, split).afold(f), subList(split, size()).afold(f))
	    
	def each() =
		repeat(IterableToStream(underlying))
	
	def eachWithIndex() =
		def h(stream, i) = stream() >v> ((v, i) | h(stream, i+1))
		h(IterableToStream(underlying), 0)
	
	def copy() =
		ArrayList_wrap(JCollections.synchronizedList(JArrayList(underlying)))
	
	def tail() = subList(1, size())
	
	def toString() = 
		val es = map(lambda(x) = x.toString() ; x >(x, y)> (x.toString(), y.toString()) ; x).afold({ _ + ", " + _ }) ; ""
		"ArrayList([" + es + "])"
}

val ArrayList = new {
	val apply = ArrayList_apply
	val withSizeHint = ArrayList_withSizeHint
	val fill = ArrayList_fill
	val wrap = ArrayList_wrap
	
	def collect(f) =
		val l = apply() 
		f() >e> l.add(e) >> stop;
		l
	
	def fromList(in) =
		val l = apply()
		def h([]) = l
		def h(x:xs) =
			l.add(x) >> h(xs) 
		h(in)
	
	def fromIterable(in) =
		val l = apply()
		val str = IterableToStream(in)
		def h() =
			l.add(str()) >> h() ; l 
		h()
}

import class JConcurrentHashMap = "java.util.concurrent.ConcurrentHashMap"
import class JSet = "java.util.Set"

	
def Map_wrap(s) = new Map {
	val underlying = s
}

{-
A concurrent map.

This does not support null values.
-}
class Map {
  val underlying :: JConcurrentHashMap
  
  val put = underlying.put
  val remove = underlying.remove
  val size = underlying.size
    
  def getOrUpdate(k, f :: lambda() :: Top) = 
    get(k) ; underlying.putIfAbsent(k, f()) >> underlying.get(k)
    
  def get(k) = 
    val v = underlying.get(k)
    Iff(v = null) >> v
    
  def each() = 
    val stream = IterableToStream(underlying.entrySet())
    repeat(stream) >e> (e.getKey(), e.getValue())
    
  def eachValue() = 
    val stream = IterableToStream(underlying.values())
    repeat(stream)

  def copy() = Map_wrap(JConcurrentHashMap(underlying))
		
  def toString() =
    val es = ArrayList.collect({ each() >(k, v)> k.toString() + " -> " + v.toString() }).afold({ _ + ", " + _ }) ; ""
    "Map({" + es + "})"
}

val Map = new {
	def apply() = wrap(JConcurrentHashMap())
	val wrap = Map_wrap
}


def Set_apply() = Set_wrap(JConcurrentHashMap.newKeySet())

def Set_wrap(s) = new Set {
	val underlying = s
}

{-
A concurrent set.

This does not support null values.
-}
class Set {
  val underlying :: JSet
  
  val contains = underlying.contains
  val add = underlying.add
  val remove = underlying.remove
  val size = underlying.size
    
  def each() =
    val stream = IterableToStream(underlying)
    repeat(stream)

  def map(f) = 
    val res = Set_apply()
    each() >x> res.add(f(x)) >> stop ;
    res

	def fold(f) =
		val ch = Channel()
	    val stream = IterableToStream(underlying)
		def work(i) =
			val x = stream()
			val y = x >> stream()
			x >> y >> (ch.put(f(x,y)) >> stop | work(i+1)) ;
			x >> (ch.put(x) >> stop | work(i+1)) ;
			finish(i)
	    def finish(i) =
			if (i <: 2) then ch.get()
			else ch.get() >x> ch.get() >y>
				( ch.put(f(x,y)) >> stop | finish(i-1) )
		work(0)
		
  def toString() = 
    val es = ArrayList.collect({ each() >v> v.toString() }).afold({ _ + ", " + _ })
    "Set({" + es + "})"
}

val Set = new {
	val apply = Set_apply
	val wrap = Set_wrap
}

import class ComparatorFromMap = "orc.test.item.scalabenchmarks.fpgrowth.ComparatorFromMap"

class SupportMap extends Map {
	val derivedComparator = ComparatorFromMap(underlying)

	def sortBySupport(l :: ArrayList) =
		JCollections.sort(l.underlying, derivedComparator) 
	
	def normalize(l :: ArrayList) =
		def findFirstInfreq(i, l) if (l.size() :> 0) =
			get(l.head()) >> findFirstInfreq(i + 1, l.tail()) ; i
		def findFirstInfreq(i, l) = i
		sortBySupport(l) >>
		l.subList(0, findFirstInfreq(0, l))
		
	def put(transaction, support) = 
		transaction.each() >item> 
			getOrUpdate(item, { LongAdder() }).add(support) >> stop ;
		signal
		
	def removeByThreshold(threshold) =
		each() >(k, v)> 
			Iff(v >= threshold) >> remove(k) >> stop ;
		signal 
}

val SupportMap = new {
	def empty() = new SupportMap {
		val underlying = JConcurrentHashMap()
	}

	def apply(db, threshold) =
		val supportMap = empty()
		db.each() >transaction>
			supportMap.put(transaction, 1) >> stop ;
		supportMap.removeByThreshold(threshold) >>
		supportMap
	
	def fromPatterns(db, threshold) =
		val supportMap = empty()
		db.each() >(transaction, support)>
			supportMap.put(transaction, support) >> stop ;
		supportMap.removeByThreshold(threshold) >>
		supportMap
}	
	
class FPNode {
	val itemName :: String
	val count :: LongAdder = LongAdder()
	val parent :: FPNode
	
	val children :: Map = Map()
	
	def getChild(i, tree :: FPTree) =
		val outer = this
		children.getOrUpdate(i, { 
			new FPNode {
				val itemName = i
				val parent = outer
			}
		})
		--children.each() >c> Ift(c.itemName = i) >> c 
	
	def addCount(n) = count.add(n)
	
	def getSinglePath() = 
		Ift(children.size() = 0) >> [this] |
		Ift(children.size() = 1) >> this : children.underlying.values().iterator().next().getSinglePath()
		
	def toTreeString() = 
		val es = ArrayList.collect({ children.each() >(k, v)> k.toString() + " -> " + v.toTreeString() }).afold({ _ + ", " + _ }) ; ""
		"FPNode(" + itemName + ":" + count.sum() + " {\n" + es + "\n})"
		
	def toString() = 
		"FPNode(" + itemName + ":" + count.sum() + ")"
}

def allSublists([]) = []
def allSublists(x:xs) =
	allSublists(xs) >ys> (x : ys | ys) >(_:_) as l> l 

def FPTree_fromPatterns(base, threshold) =
	val support = SupportMap.fromPatterns(base, threshold)
	val t = new FPTree
	base.each() >(transaction, count)>
		t.addPattern(support.normalize(transaction), count) >> stop ;
	t

class FPTree {
	val root :: FPNode = new FPNode {
		val itemName = "<root>"
		val parent = stop
		def getSinglePath() = tail(super.getSinglePath()) 
	}

	def toString() = 
		"FPTree " + root.toTreeString()
	
	val itemsToNodes :: Map = Map()
	
	def addNode(node) = itemsToNodes.getOrUpdate(node.itemName, { Set() }).add(node)

	def addTransaction(transaction) = addPattern(transaction, 1)
		
	def addPattern(pattern, support) =
		def h(pat, node) if (pat.size() :> 0) =
			val next = node.getChild(pat.head(), this)
			val _ = addNode(next)
			next.addCount(support) >>
			h(pat.tail(), next)
		h(pattern, root)
	
	def getPathTo(node) if (node = root) = []
	def getPathTo(node) = node : getPathTo(node.parent)
		
	def isEmpty() = root.children.size() = 0
	
	def growth(pattern, threshold) =
		--Println(pattern.toString() + " -- this: " + this.toString()) >>
		-- If there is a single path then output every subset.
		root.getSinglePath() >(_:_) as path>
			--Println(pattern.toString() + " -- path: " + ArrayList.fromList(path).toString()) >>
			allSublists(path) >p> (
				val resultPattern = Map()
				val support = cfold(min, map({ _.count.sum() }, p)) ; 0
				each(p) >i> resultPattern.put(i.itemName, support) >> stop |
				pattern.each() >(i, _)> resultPattern.put(i, support) >> stop ;
				--resultPattern.remove(root.itemName) >>
				resultPattern
			) ;
		
		-- Generate new subtrees.
		itemsToNodes.each() >(item, nodes)> (
			-- Build subpattern
			val subpattern = (
				val subpattern = Map()
				val support = nodes.map({ _.count.sum() }).fold({ _ + _ })
				subpattern.put(item, support) >> stop |
				pattern.each() >(k, _)> subpattern.put(k, support) >> stop ;
				subpattern
			)
			
			-- Build conditional subpattern base
			val base = ArrayList.collect({
				--Println(pattern.toString() + " " + item + " -- nodes: " + nodes.toString()) >>
				nodes.each() >n>
					getPathTo(n) >path> --filter({ nodes.subList(0, i).contains(_) }) >prefix>
					--Println(pattern.toString() + " " + item + " -- " + n.itemName + " -- path: " + ArrayList.fromList(path).toString()) >>
					(ArrayList.fromList(map({ _.itemName }, tail(path))), head(path).count.sum())
			})
			
			-- Compute the subpattern tree
			val t = FPTree_fromPatterns(base, threshold)
			
			--Println(pattern.toString() + " " + item + " -- subpattern: " + subpattern.toString()) >>
			--Println(pattern.toString() + " " + item + " -- base: " + base.toString()) >>
			--Println(pattern.toString() + " " + item + " -- t: " + t.toString()) >>
			
			-- Recursive call if the tree is non-empty
			Iff(t.isEmpty()) >> t.growth(subpattern, threshold)
		)
}

val FPTree = new {
	def apply(db, threshold) =
		--val _ = Println(db.toString())
		val support = SupportMap(db, threshold)
		--val _ = Println(support.toString())
		val t = new FPTree
		db.each() >transaction>
			t.addTransaction(support.normalize(transaction)) >> stop ;
		t

	val fromPatterns = FPTree_fromPatterns
}

import class FreqMineData = "orc.test.item.scalabenchmarks.fpgrowth.FreqMineData"
import class FrequentItemSet = "orc.test.item.scalabenchmarks.fpgrowth.FrequentItemSet"


-- Test data

def Transaction(s) =
	val l = ArrayList.withSizeHint(s.length())
	upto(s.length()) >i> l.add(s.substring(i, i+1)) >> stop ;
	l

val testdb = ArrayList.fromList([
	Transaction("facdgimp"),
	Transaction("abcflmoz"),
	Transaction("bfhjo"),
	Transaction("bcksp"),
	Transaction("facdgsimp"),
	Transaction("abcflmo"),
	Transaction("bfhjo"),
	Transaction("bcksp"),
	Transaction("facdgimpz"),
	Transaction("abcflmo"),
	Transaction("bfhjo"),
	Transaction("bcksp"),
	Transaction("afcelpmnz")
	])

-- Benchmark

benchmarkSized("FP-Growth-naive", FreqMineData.dataSize(),
	{ ArrayList.wrap(FreqMineData.generate()).map(ArrayList.wrap) },
	(lambda(d) = 
		val threshold = (FreqMineData.nTransactions() * 0.02).longValue()
		Println(threshold) >>
		ArrayList.collect({ FPTree(d, threshold).growth(Map(), threshold) >itemSet> FrequentItemSet(itemSet.underlying.keySet(), itemSet.eachValue()) }).underlying
	),
	FreqMineData.check)
