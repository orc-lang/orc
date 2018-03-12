//
// AnalysisCache.scala -- Scala class AnalysisCache and trait AnalysisRunner
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile

import scala.ref.WeakReference
import scala.ref.ReferenceQueue

class AnalysisCache {
  private val cache = collection.mutable.HashMap[AnalysisCacheKey, Any]()

  def get[P <: Product, T](runner: AnalysisRunner[P, T])(params: P): T = {
    incrClean()
    cache.getOrElseUpdate(new AnalysisCacheKey(runner, params), runner.compute(this)(params)).asInstanceOf[T]
  }

  def clean(): Unit = {
    cache.clear()
  }

  private class AssociatedWeakRef(v: AnyRef, val key: AnalysisCacheKey, q: ReferenceQueue[AnyRef]) extends WeakReference[AnyRef](v, q)

  private class AnalysisCacheKey(val analysis: AnalysisRunner[_, _], params1: Product) {
    val params = params1.productIterator.map(v => new AssociatedWeakRef(v.asInstanceOf[AnyRef], this, queue)).toSeq
    override val hashCode = analysis.hashCode() + params1.productIterator.map(_.hashCode()).sum
    override def equals(o: Any) = o match {
      case o: AnalysisCacheKey => {
        analysis == o.analysis &&
          (params zip o.params).forall({ p =>
            p._1.get == p._2.get
          })
      }
      case _ => false
    }
  }

  val queue = new ReferenceQueue[AnyRef]()

  def incrClean(n: Int = 10): Unit = {
    if (n > 0) {
      queue.poll match {
        case Some(k: AssociatedWeakRef) =>
          cache -= k.key
          incrClean(n - 1)
        case Some(_) =>
          throw new AssertionError("Not an AssociatedWeakRef")
        case None =>
          ()
      }
    }
  }
}

trait AnalysisRunner[P <: Product, T] {
  def compute(cache: AnalysisCache)(params: P): T
}
