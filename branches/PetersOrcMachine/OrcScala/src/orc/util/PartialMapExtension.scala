//
// PartialMapExtension.scala -- Scala object PartialMapExtension
// Project OrcScala
//
// $Id$
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

/**
  * Adds the partialMap method to Lists.
  *
  * xs.partialMap(f) returns a new list where each element
  * x of xs is mapped to:
  *
  * f(x) if x is in the domain of f
  * x otherwise.
  *
  * @author dkitchin
  */
object PartialMapExtension {

  // Adds a partialMap method to lists
  class ListWithPartialMap[A](xs: List[A]) {
    def partialMap(f: PartialFunction[A, A]): List[A] = {
      xs map { elem: A => if (f.isDefinedAt(elem)) { f(elem) } else { elem } }
    }
  }

  implicit def addPartialMapToList[A](xs: List[A]) = new ListWithPartialMap(xs)

}
