//
// Vertex.scala -- Scala traits Vertex and Edge, and classes VertexWithPathLen and EdgeWithIntWeight
// Project OrcTests
//
// Created by jthywiss on Mar 15, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test.item.distrib

import orc.lib.state.Ref
import orc.lib.state.Semaphore

/** Generic digraph vertex. */
trait Vertex[N, E] {
  val name: N
  val outEdges: List[E]
}

/** Generic digraph edge, with a weight. */
trait Edge[N, W] {
  val head: N
  val tail: N
  val weight: W
}

/** Vertex class for SSSP; includes path length (weight accumulator) Ref and a Semaphore. */
case class VertexWithPathLen(name: Int, outEdges: List[EdgeWithIntWeight], pathLen: Ref.RefInstance, pathLenSemaphore: Semaphore#SemaphoreInstance) extends Vertex[Int, EdgeWithIntWeight] { }

/** Edge class for SSSP. */
case class EdgeWithIntWeight(head: Int, tail: Int, weight: Int) extends Edge[Int, Int] { }