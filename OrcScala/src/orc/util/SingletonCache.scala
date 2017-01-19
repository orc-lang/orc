//
// SingletonCache.scala -- Scala class/trait/object SingletonCache
// Project OrcScala
//
// Created by amp on Jan 27, 2014.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

import scala.collection.mutable
import scala.ref.WeakReference

/**
  * @author amp
  */
class SingletonCache[T <: AnyRef] {
  val cache = mutable.WeakHashMap[T, WeakReference[T]]()

  def normalize(c: T): T = {
    cache.get(c).getOrElse {
      val wc = WeakReference(c)
      cache += c -> wc
      wc
    }.get.getOrElse(c)
  }

  def size = cache.size

  def items = cache.keys

  def clear() {
    cache.clear()
  }
}
