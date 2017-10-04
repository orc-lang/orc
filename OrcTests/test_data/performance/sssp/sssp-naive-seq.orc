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

import class Node = "orc.test.item.scalabenchmarks.SSSPNode"
import class Edge = "orc.test.item.scalabenchmarks.SSSPEdge"
import class SSSP = "orc.test.item.scalabenchmarks.SSSP"

import class AtomicLong = "java.util.concurrent.atomic.AtomicLong"

val counter = AtomicLong(0)

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
	repeat({ Sequentialize() >> queue.getD() >index> counter.getAndIncrement() >> (
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

benchmarkSized(nodes.length? * nodes.length?, { 
	val r = sssp(nodes, edges, source)
	Println((r(0)?, r(1)?, r(2)?, r(3)?, r(4)?)) >>
	Println(counter.get())
})

