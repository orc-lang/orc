//
// TypeListEnrichment.scala -- Scala object TypeListEnrichment
// Project OrcScala
//
// Created by dkitchin on Jan 30, 2013.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

import scala.language.implicitConversions

import orc.types.Type
import orc.types.SignalType
import orc.types.TupleType

/** Extend join, meet, and < to lists of types.
  *
  * @author dkitchin
  */
object TypeListEnrichment {

  class RichTypeList(types: List[Type]) {

    def join(otherTypes: List[Type]): List[Type] = {
      (types, otherTypes).zipped map { case (t, u) => t join u }
    }

    def meet(otherTypes: List[Type]): List[Type] = {
      (types, otherTypes).zipped map { case (t, u) => t meet u }
    }

    def <(otherTypes: List[Type]): Boolean = {
      (types, otherTypes).zipped forall { case (t, u) => t < u }
    }

    def condense: Type = {
      types match {
        case Nil => SignalType
        case List(t) => t
        case ts => TupleType(ts)
      }
    }

  }

  implicit def enrichTypeList(types: List[Type]): RichTypeList = new RichTypeList(types)

}
