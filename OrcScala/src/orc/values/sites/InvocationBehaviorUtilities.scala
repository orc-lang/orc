//
// InvocationBehaviorUtilities.scala -- Scala object InvocationBehaviorUtilities
// Project OrcScala
//
// Created by amp on July, 2017.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import scala.reflect.ClassTag
import java.lang.ClassCastException

/** A collection of utility methods for writing invokers and accessors.
  */
object InvocationBehaviorUtilities {
  /** True iff arg is of the type specified as cls.
    *
    * A null value in cls matches only exactly null in arg.
    */
  @inline
  final def valueHasType(arg: AnyRef, cls: Class[_]): Boolean = {
    (arg == null && cls == null) ||
    (arg == null && cls != null && !cls.isPrimitive()) ||
    (arg != null && cls != null && cls.isInstance(arg))
  }

  /** Cast arg to type cls explicitly.
    *
    * cls may be null in which case arg must be null.
    */
  @inline
  final def castValue[T](arg: AnyRef, cls: Class[T]): T = {
    if (cls == null) {
      if(arg != null)
        throw new ClassCastException(arg.toString)
      null.asInstanceOf[T]
    } else {
      cls.cast(arg)
    }
  }

  @inline
  final def valueHasTypeByTag[A : ClassTag](arg: AnyRef): Boolean = {
    val cls = implicitly[ClassTag[A]].runtimeClass
    valueHasType(arg, cls)
  }

  /** True iff arguments are all of the same type as the matching class in argumentClss.
    *
    * A null value in argumentClss matches only exactly null in arguments.
    */
  @inline
  final def valuesHaveType(arguments: Array[AnyRef], argumentClss: Array[Class[_]]): Boolean = {
    arguments.length == argumentClss.length && {
      // Conceptually: (argumentClss zip arguments).forall(... Predicate ...)
      // But this is hot path, so.... Sorry.
      var i = 0
      var res = true
      while (i < arguments.length && res) {
        // Predicate here:
        res &&= valueHasType(arguments(i), argumentClss(i))
        i += 1
      }
      res
    }
  }

  /** Get the class of v, or null if v == null.
    */
  @inline
  final def valueType(v: Any): Class[_] = {
    if (v == null)
      null
    else
      v.getClass()
  }
}


