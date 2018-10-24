//
// IterableToStream.scala -- Scala object IterableToStream
// Project OrcScala
//
// Created by dkitchin on Apr 11, 2011.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util

import java.lang.{ Iterable => JIterable }

import orc.compile.typecheck.Typeloader
import orc.error.runtime.ArgumentTypeMismatchException
import orc.types.{ FunctionType, SimpleFunctionType, TypeVariable }
import orc.values.sites.{ LocalSingletonSite, TypedSite }
import orc.values.sites.compatibility.{ PartialSite0, TotalSite1 }

/** @author dkitchin
  */
object IterableToStream extends TotalSite1 with TypedSite with Serializable with LocalSingletonSite {

  final class IterableNext(iter: Iterator[_]) extends PartialSite0 {
    def eval() =
      if (iter.hasNext) {
        Some(iter.next().asInstanceOf[AnyRef])
      } else {
        None
      }
  }


  def eval(arg: AnyRef) = {
    arg match {
      case i: JIterable[_] => {
        // Java Iterables
        import scala.collection.JavaConverters._
        new IterableNext(i.iterator.asScala)
      }
      case i: Iterable[_] => {
        // Scala Iterables
        new IterableNext(i.iterator)
      }
      case i: Array[_] => {
        // Arrays
        new IterableNext(i.iterator)
      }
      case a => throw new ArgumentTypeMismatchException(0, "Iterable", if (a != null) a.getClass().toString() else "null")
    }
  }

  def orcType() = {
    val X = new TypeVariable()
    // TODO: This type is more restrictive than the implementation.
    val Iterable = Typeloader.liftJavaTypeOperator(classOf[Iterable[_]])
    FunctionType(List(X), List(Iterable(X)), SimpleFunctionType(X))
  }

}
