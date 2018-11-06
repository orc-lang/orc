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

import orc.OrcRuntime
import orc.compile.typecheck.Typeloader
import orc.types.{ FunctionType, SimpleFunctionType, TypeVariable }
import orc.values.sites.{ LocalSingletonSite, PartialSite0Simple, TotalSite1Base, TypedSite }

/** @author dkitchin
  */
object IterableToStream extends TotalSite1Base[AnyRef] with TypedSite with Serializable with LocalSingletonSite {

  final class IterableNext(iter: Iterator[_]) extends PartialSite0Simple {
    def eval() =
      if (iter.hasNext) {
        Some(iter.next().asInstanceOf[AnyRef])
      } else {
        None
      }
  }


  def getInvoker(runtime: OrcRuntime, arg: AnyRef) = {
    arg match {
      case i: JIterable[_] => {
        // Java Iterables
        import scala.collection.JavaConverters._
        invoker(this, i) { (_, i) => new IterableNext(i.iterator.asScala) }
      }
      case i: Iterable[_] => {
        // Scala Iterables
        invoker(this, i) { (_, i) => new IterableNext(i.iterator) }
      }
      case i: Array[_] => {
        // Arrays
        invoker(this, i) { (_, i) => new IterableNext(i.iterator) }
      }
    }
  }

  def orcType() = {
    val X = new TypeVariable()
    // TODO: This type is more restrictive than the implementation.
    val Iterable = Typeloader.liftJavaTypeOperator(classOf[Iterable[_]])
    FunctionType(List(X), List(Iterable(X)), SimpleFunctionType(X))
  }

}
