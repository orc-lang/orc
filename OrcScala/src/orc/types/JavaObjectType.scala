//
// JavaObjectType.scala -- Scala class JavaObjectType
// Project OrcScala
//
// Created by dkitchin on Nov 29, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import java.lang.{ reflect => jvm }

import orc.error.compiletime.typing.UncallableTypeException

/** The type of a Java object.
  *
  * @author dkitchin
  */
case class JavaObjectType(val cl: Class[_], javaContext: Map[jvm.TypeVariable[_], Type] = Nil.toMap) extends CallableType with JavaType with StrictCallableType with HasMembersType {

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
    // TODO: I have no idea what I should be doing here.
    throw new UncallableTypeException(this);
  }

  def getMember(f: FieldType): Type = {
    typeOfMember(f.f, false, javaContext)
  }
}
