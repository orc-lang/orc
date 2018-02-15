package orc.test.item.scalabenchmarks.sssp

import scala.util.Random

case class SSSPNode(initialEdge: Int, nEdges: Int) {
  def edges(edges: Array[SSSPEdge]) = edges.view(initialEdge, initialEdge + nEdges)
}

case class SSSPEdge(to: Int, cost: Int) {
}

object SSSPData {
  
  val minEdges = 8
  val maxInitEdges = 20
  val minWeight = 1
  val maxWeight = 10
  
  def generate(nNodes: Int): (Array[SSSPNode], Array[SSSPEdge], Int) = {
    val rnd = new Random(1) // Fixed seed PRNG
    
    val edges = new collection.mutable.ArrayBuffer[SSSPEdge](nNodes * 4)
    def makeNode() = {
      val nEdges = rnd.nextInt(maxInitEdges - minEdges + 1) + minEdges
      val node = SSSPNode(edges.size, nEdges)
      edges ++= (0 until nEdges).map { _ =>
        SSSPEdge(rnd.nextInt(nNodes), rnd.nextInt(maxWeight - minWeight) + minEdges)
      }
      node
    }
    
    (Array.fill(nNodes)(makeNode()), edges.toArray, rnd.nextInt(nNodes))
  }
}