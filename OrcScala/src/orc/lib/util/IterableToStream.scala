//
// IterableToStream.scala -- Scala object IterableToStream
// Project OrcScala
//
// Created by dkitchin on Apr 11, 2011.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.util

import java.lang.Iterable

import orc.compile.typecheck.Typeloader
import orc.error.runtime.ArgumentTypeMismatchException
import orc.types.{ FunctionType, SimpleFunctionType, TypeVariable }
import orc.values.sites.{ PartialSite0, TotalSite1, TypedSite }

/** @author dkitchin
  */
object IterableToStream extends TotalSite1 with TypedSite {

  def eval(arg: AnyRef) = {
    arg match {
      case i: Iterable[_] => {
        val iter = i.iterator()
        new PartialSite0 {
          def eval() =
            if (iter.hasNext()) {
              Some(iter.next().asInstanceOf[AnyRef])
            } else {
              None
            }
        }
      }
      case a => throw new ArgumentTypeMismatchException(0, "Iterable", if (a != null) a.getClass().toString() else "null")
    }
  }

  def orcType() = {
    val X = new TypeVariable()
    val Iterable = Typeloader.liftJavaTypeOperator(classOf[Iterable[_]])
    FunctionType(List(X), List(Iterable(X)), SimpleFunctionType(X))
  }

}
