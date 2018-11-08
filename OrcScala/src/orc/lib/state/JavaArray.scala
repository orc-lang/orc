//
// JavaArray.java -- Java class JavaArray
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.state

import java.lang.reflect.{Array => JArray}

import orc.{DirectInvoker, OrcRuntime}
import orc.error.runtime.{ArityMismatchException, BadArrayElementTypeException}
import orc.lib.state.types.ArrayType
import orc.types.Type
import orc.util.ArrayExtensions.{Array1, Array2}
import orc.values.sites.{
  TargetClassAndArgumentClassSpecializedInvoker,
  TotalSiteBase,
  TypedSite
}

object JavaArray extends TotalSiteBase with TypedSite {
  override val inlinable = true
  private val types: Map[String, Class[_]] = Map(
    ("double", java.lang.Double.TYPE),
    ("float", java.lang.Float.TYPE),
    ("long", java.lang.Long.TYPE),
    ("int", java.lang.Integer.TYPE),
    ("short", java.lang.Short.TYPE),
    ("byte", java.lang.Byte.TYPE),
    ("char", java.lang.Character.TYPE),
    ("boolean", java.lang.Boolean.TYPE),
  )

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]) = {
    args.size match {
      case 1 =>
        invoker(this, args: _*) { (_, args) =>
          val Array1(i: Number) = args
          JArray.newInstance(classOf[AnyRef], i.intValue())
        }
      case 2 =>
        invoker(this, args: _*) { (_, args) =>
          val Array2(i: Number, s: String) = args
          val tpe: Class[_] = types.getOrElse(s, {
            throw new BadArrayElementTypeException(s)
          })
          JArray.newInstance(tpe, i.intValue())
        }
      case _ =>
        new TargetClassAndArgumentClassSpecializedInvoker(this, args)
        with DirectInvoker {
          @throws[Throwable]
          def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
            throw new ArityMismatchException(2, args.size)
          }
        }
    }
  }

  override def orcType(): Type = ArrayType.getBuilder
}
