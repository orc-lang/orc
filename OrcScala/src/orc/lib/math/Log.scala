//
// Log2.scala -- Scala object Log2
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.math

import scala.reflect.ClassTag

import orc.values.sites.InvokerMethod
import orc.OrcRuntime
import orc.Invoker
import orc.OnlyDirectInvoker
import orc.IllegalArgumentInvoker

final class LogSpecializedInvoker[T <: Number : ClassTag] extends OnlyDirectInvoker {
  val cls = implicitly[ClassTag[T]].runtimeClass
  
  def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
    target == Log && arguments.length == 1 && cls.isInstance(arguments(0)) 
  }

  def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
    Math.log(cls.cast(arguments(0)).asInstanceOf[T].doubleValue()).asInstanceOf[AnyRef]
  }
}


object Log extends InvokerMethod {
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length == 1 && args(0).isInstanceOf[Number]) {
      args(0) match {
        case _: Integer => new LogSpecializedInvoker[Integer]
        case _: java.lang.Double => new LogSpecializedInvoker[java.lang.Double]
        case _: BigInt => new LogSpecializedInvoker[BigInt]
        case _: BigDecimal => new LogSpecializedInvoker[BigDecimal]
        case _: Number => new LogSpecializedInvoker[Number]
      }
    } else {
      IllegalArgumentInvoker(this, args)
    }
  }
}
