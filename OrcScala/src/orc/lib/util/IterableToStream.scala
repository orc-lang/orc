//
// IterableToStream.scala -- Scala object IterableToStream
// Project OrcScala
//
// Created by dkitchin on Apr 11, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.util

import orc.values.sites.Site
import orc.values.sites.TypedSite
import orc.values.sites.TotalSite1
import orc.values.sites.PartialSite0
import java.lang.Iterable
import orc.compile.typecheck.Typeloader
import orc.lib.builtin.structured.ListType
import orc.types.TypeVariable
import orc.types.FunctionType
import orc.types.SimpleFunctionType
import orc.error.runtime.ArgumentTypeMismatchException

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
