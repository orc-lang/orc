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

def sforBy(low, high, step, f) = Sequentialize() >> (
  if low >= high then signal
  else ( f(low) >> sforBy(low+step, high, step, f) )
  )

def sfor(low, high, f) = Sequentialize() >> sforBy(low, high, 1, f)

def sssp(nodes :: Array[Node], edges :: Array[Edge], source :: Integer) =
	val outQLock = Semaphore(1)
	val q1 = ArrayList()
	val q2 = ArrayList()
    val result = (Sequentialize() >>
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
    
	def processNode(index, localQ, localQLock, gray) = Sequentialize() >> (
		--val _ = Println("processNode " + index + " " + gray)
		val node = nodes(index)?
		val currentCost = result.get(index)
		{-
		sfor(node.initialEdge(), node.initialEdge() + node.nEdges(), lambda(edgeIndex) = edges(edgeIndex)? >edge> (
			val to = edge.to()
			val newCost = currentCost + edge.cost()
			val oldCost = getAndMinResult(to, newCost)
			Ift(newCost <: oldCost) >> Ift(colors.getAndSet(to, gray) /= gray) >>
				localQ.add(to) >> stop ;
				signal
		)) >>
		signal
		-}
		--val _ = Println("processNode " + index + " " + gray + " " + nodes.length? + " " + edges.length?)
		--val _ = Println("processNode " + (node.initialEdge(), node.initialEdge() + node.nEdges()))
		for(node.initialEdge(), node.initialEdge() + node.nEdges()) >edgeIndex> edges(edgeIndex)? >edge> (
			val to = edge.to()
			val newCost = currentCost + edge.cost()
			val oldCost = getAndMinResult(to, newCost)
			Ift(newCost <: oldCost) >> Ift(colors.getAndSet(to, gray) /= gray) >>
				withLock(localQLock, { localQ.add(to) })
		) >> stop ;
		signal
	)
	
	def processBatch(inQ, indexStart, indexEnd, outQ, gray) = Sequentialize() >> (
		val _ = Println("processBatch " + indexStart + " " + indexEnd + " " + gray + " " + inQ.size())
		val localQ = ArrayList()
		val localLock = Semaphore(1)
		sfor(indexStart, indexEnd, lambda(index) = processNode(inQ.get(index), localQ, localLock, gray)) >>
		withLock(outQLock, { outQ.addAll(localQ) })
	)
	
	def h(inQ, outQ, gray) =
		val partitionSize = max(Ceil((0.0 + inQ.size()) / nPartitions), 512)
		forBy(0, inQ.size(), partitionSize) >partitionIndex> Sequentialize() >>
		  processBatch(inQ, partitionIndex, min(partitionIndex + partitionSize, inQ.size()), outQ, gray) >>
		  stop ;
		  
		inQ.clear() >> Println("Batch " + gray + " done " + outQ.size()) >> (
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

benchmarkSized("SSSP-batched-partitioned", nodes.length? * nodes.length?, { nodes >> edges >> source }, { _ >> sssp(nodes, edges, source) })

{-
BENCHMARK
-}

