{- sssp.orc

Single-Source Shortest Path

Implemented using BFS.

This is a naive implementation which uses mutable arrays but is
otherwise not optimized. It does not implement edge weights since
non-weighted SSSP scales better using simple algorithms.

-}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

import class Node = "orc.test.item.scalabenchmarks.sssp.SSSPNode"
import class Edge = "orc.test.item.scalabenchmarks.sssp.SSSPEdge"
import class SSSP = "orc.test.item.scalabenchmarks.sssp.SSSP"

import class AtomicLong = "java.util.concurrent.atomic.AtomicLong"

def sssp(nodes :: Array[Node], edges :: Array[Edge], source :: Integer) =
	val queue = Channel()
    val result = (
      Array(nodes.length?) >a> (
      upto(a.length?) >i>
      a(i) := 1073741824 >>
      stop ;
      a)
    )
    result(source) := 0 >>
	queue.put(source) >>
	repeat({ Sequentialize() >> queue.getD() >index> (
		val node = nodes(index)?
		val currentCost = result(index)?
		for(node.initialEdge(), node.initialEdge() + node.nEdges()) >edgeIndex> edges(edgeIndex)? >edge> (
			val to = edge.to()
			val newCost = currentCost + edge.cost()
			if newCost <: result(to)? then
				result(to) := newCost >>
				queue.put(to)
			else
				signal
		) >> stop ;
		signal
	)}) >> stop ;
	result

val nodes = SSSP.nodes()
val edges = SSSP.edges()
val source = SSSP.source()


benchmarkSized("SSSP-naive-seq", nodes.length? * nodes.length?, { nodes >> edges >> source }, { _ >> sssp(nodes, edges, source) }, SSSP.check)

{-
BENCHMARK
-}

