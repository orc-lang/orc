//
// BlockableMapExtension.scala -- Scala object BlockableMapExtension
// Project OrcScala
//
// Created by dkitchin on May 19, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import scala.language.implicitConversions

/** Adds the blockableMap method to Lists.
  *
  * xs.blockableMap(f)(k) calls f(x)(g) on each element x of xs,
  * where g is a function that captures a mapped value y and
  * continues the evaluation of blockableMap.
  *
  * Once the evaluation of blockableMap reaches the end of the list,
  * k is invoked on the assembled list of mapped values y.
  *
  * This method is used to map over a list of potentially blocking
  * entities. It uses the continuation g to resume mapping after blocking
  * on an element.
  *
  * @author dkitchin
  */
object BlockableMapExtension {

  class ListWithBlockableMap[X](xs: List[X]) {
    def blockableMap[Y](f: X => (Y => Unit) => Unit)(k: List[Y] => Unit) {
      def walk(xs: List[X], ys: List[Y]) {
        xs match {
          case z :: zs => f(z) { y: Y => walk(zs, y :: ys) }
          case Nil => k(ys.reverse)
        }
      }
      walk(xs, Nil)
    }
  }

  implicit def addBlockableMapToList[A](xs: List[A]) = new ListWithBlockableMap(xs)

}
