//
// JvmObjectType.scala -- Scala class JvmObjectType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 29, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import scala.language.existentials
import java.lang.{ reflect => jvm }
import orc.compile.typecheck.Typeloader._
import orc.error.compiletime.typing.TypeArgumentArityException
import orc.error.compiletime.typing.UncallableTypeException
import orc.error.compiletime.typing.NoSuchMemberException

/** The type of a Java object.
  *
  * @author dkitchin
  */
case class JavaObjectType(val cl: Class[_], javaContext: Map[jvm.TypeVariable[_], Type] = Nil.toMap) extends CallableType with JavaType with StrictType {

  override def toString = Option(cl.getCanonicalName).getOrElse(cl.getName)

  /* JVM object types do not yet have an implementation of join or meet.
   * Such an implementation would require a time-intensive traversal of
   * the Java class hierarchy. In some cases, it may not even be possible
   * to find a suitable result.
   * 
   * The absence of a least supertype join or greatest subtype meet does
   * not affect the correctness of type checking, just the effectiveness
   * of inference.
   */

  // def join ...
  // def meet ...

  override def <(that: Type): Boolean = {
    that match {
      case JavaObjectType(otherCl, otherContext) => {
        (otherCl isAssignableFrom cl) &&
          {
            val commonVars = (javaContext.keySet) intersect (otherContext.keySet)
            commonVars forall { x => (javaContext(x)) equals (otherContext(x)) }
          }
      }
      case _ => super.<(that)
    }
  }

  def call(typeArgs: List[Type], argTypes: List[Type]): Type = {
    argTypes match {
      case List(FieldType(name)) => {
        if (typeArgs.size > 0) {
          throw new TypeArgumentArityException(0, typeArgs.size)
        }
        typeOfMember(name, false, javaContext)
      }
      case _ => {
        typeOfMember("apply", false, javaContext) match {
          case ct: CallableType => ct.call(typeArgs, argTypes)
          case _ => throw new UncallableTypeException(this)
        }
      }
    }
  }

}
