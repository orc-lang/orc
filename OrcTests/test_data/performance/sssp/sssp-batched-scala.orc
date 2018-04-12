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

import class Node = "orc.test.item.scalabenchmarks.sssp.SSSPNode"
import class Edge = "orc.test.item.scalabenchmarks.sssp.SSSPEdge"
import class SSSP = "orc.test.item.scalabenchmarks.sssp.SSSP"
import class SSSPBatchedPar = "orc.test.item.scalabenchmarks.sssp.SSSPBatchedPar"

import class AtomicLong = "java.util.concurrent.atomic.AtomicLong"
import class AtomicIntegerArray = "java.util.concurrent.atomic.AtomicIntegerArray"
import class ArrayList = "java.util.ArrayList"

def eachArrayList(a) = upto(a.size()) >i> a.get(i)

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
    
	def processNode(index, outQ, gray) = (
		val node = nodes(index)?
		val currentCost = result.get(index)
		for(node.initialEdge(), node.initialEdge() + node.nEdges()) >edgeIndex> (
		    SSSPBatchedPar.processEdge(edges, colors, result, gray, edgeIndex, currentCost, outQ)
		) >> stop ;
		signal
	)
	
	def h(inQ, outQ, gray) =
		eachArrayList(inQ) >i> processNode(i, outQ, gray) >> stop ;
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


benchmarkSized("SSSP-batched-scala", nodes.length? * nodes.length?, { nodes >> edges >> source }, { _ >> sssp(nodes, edges, source) }, SSSP.check)

{-
BENCHMARK
-}

