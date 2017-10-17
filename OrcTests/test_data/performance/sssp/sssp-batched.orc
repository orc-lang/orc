{- sssp.orc

Single-Source Shortest Path

Implemented using BFS.

This implementation is based on the implementation in Parboil 
(http://impact.crhc.illinois.edu/parboil/parboil.aspx), which uses
a queue instead of marking (as in the Rodinia version).

This is a naive implementation which uses mutable arrays but is
otherwise not optimized.

-}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

import class Node = "orc.test.item.scalabenchmarks.sssp.SSSPNode"
import class Edge = "orc.test.item.scalabenchmarks.sssp.SSSPEdge"
import class SSSP = "orc.test.item.scalabenchmarks.sssp.SSSP"

import class AtomicLong = "java.util.concurrent.atomic.AtomicLong"
import class AtomicIntegerArray = "java.util.concurrent.atomic.AtomicIntegerArray"
import class ArrayList = "java.util.ArrayList"

def sssp(nodes :: Array[Node], edges :: Array[Edge], source :: Integer) =
	val outQLock = Semaphore(1)
	val q1 = ArrayList()
	val q2 = ArrayList()
    val result = (
      AtomicIntegerArray(nodes.length?) >a> (
      upto(nodes.length?) >i>
      a.set(i, 1073741824) >>
      stop ;
      a)
    )
    val colors = AtomicIntegerArray(nodes.length?)
    
    def getAndMinResult(i, v) = Sequentialize() >> (
    	val old = result.get(i)
    	if old <: v then
    		old
    	else if result.compareAndSet(i, old, v) then 
    		old
    	else
    		getAndMinResult(i, v)
    )
    
	def processNode(index, outQ, gray) = Sequentialize() >> (
		val node = nodes(index)?
		val currentCost = result.get(index)
		for(node.initialEdge(), node.initialEdge() + node.nEdges()) >edgeIndex> edges(edgeIndex)? >edge> (
			val to = edge.to()
			val newCost = currentCost + edge.cost()
			val oldCost = getAndMinResult(to, newCost)
			Ift(newCost <: oldCost) >> Ift(colors.getAndSet(to, gray) /= gray) >>
			  withLock(outQLock, { outQ.add(to) })
		) >> stop ;
		signal
	)
	
	def h(inQ, outQ, gray) =
		repeat(IterableToStream(inQ)) >i> processNode(i, outQ, gray) >> stop ;
		inQ.clear() >> ( --Println("Batch " + gray + " done " + outQ) >>
		if outQ.size() :> 0 then
			h(outQ, inQ, gray + 1)
		else
			signal
		)
	
    result.set(source, 0) >>
	q1.add(source) >>
	h(q1, q2, 1) >>
	result

val nodes = SSSP.nodes()
val edges = SSSP.edges()
val source = SSSP.source()


benchmarkSized("SSSP-batched", nodes.length? * nodes.length?, { signal }, { _ >> sssp(nodes, edges, source) })

{-
BENCHMARK
-}

