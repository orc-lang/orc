{- sssp.orc

Single-Source Shortest Path

Implemented using BFS.

This is a naive implementation which uses mutable arrays but is
otherwise not optimized. It does not implement edge weights since
non-weighted SSSP scales better using simple algorithms.

-}

include "benchmark.inc"

import class Node = "orc.test.item.scalabenchmarks.sssp.SSSPNode"
import class Edge = "orc.test.item.scalabenchmarks.sssp.SSSPEdge"
import class SSSP = "orc.test.item.scalabenchmarks.sssp.SSSP"

import class ArrayList = "java.util.ArrayList"
import class Collections = "java.util.Collections"

def eachArrayList(a) = upto(a.size()) >i> a.get(i)

def sssp(nodes :: Array[Node], edges :: Array[Integer], source :: Integer) =
	val outQLock = Semaphore(1)
	val q1 = ArrayList()
	val q2 = ArrayList()
    val result = ArrayList(Collections.nCopies(nodes.length?, 1073741824))
    val visited = ArrayList(Collections.nCopies(nodes.length?, false))
    
	def processEdge(edgeIndex, outQ, currentCost) = edges(edgeIndex)? >to> (
		--Println("Processing edge to " + to) >>
		Iff(visited.get(to)) >> 
	      (
	      --Println("Assigning " + to + " to " + currentCost + 1), 
	      visited.set(to, true),
	      result.set(to, currentCost + 1),
		  withLock(outQLock, { outQ.add(to) })
		  )
	)
	
	def processNode(index, outQ) = (
		val node = nodes(index)?
		val currentCost = result.get(index)
		--Println("Processing node " + index + " " + node + " " + currentCost) >>
		currentCost >>
		for(node.initialEdge(), node.initialEdge() + node.nEdges()) >edgeIndex> 
			processEdge(edgeIndex, outQ, currentCost) >> stop ;
		signal
	)
	
	def h(inQ, outQ) =
		eachArrayList(inQ) >i> processNode(i, outQ) >> stop ;
		inQ.clear() >> (
		if outQ.size() :> 0 then
			h(outQ, inQ)
		else
			signal
		)
	
    result.set(source, 0) >>
	q1.add(source) >>
	h(q1, q2) >>
	result

val nodes = SSSP.nodes()
val edges = SSSP.edges()
val source = SSSP.source()


benchmarkSized("SSSP-batched", nodes.length? * nodes.length?, { nodes >> edges >> source }, { _ >> sssp(nodes, edges, source) }, SSSP.check)

{-
BENCHMARK
-}

