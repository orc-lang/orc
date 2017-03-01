//
// JavaClassType.scala -- Scala class JavaClassType
// Project OrcScala
//
// Created by dkitchin on Nov 29, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import scala.language.existentials
import java.lang.{ reflect => jvm }
import orc.compile.typecheck.Typeloader._
import orc.error.NotYetImplementedException
import orc.error.compiletime.typing.TypeArgumentArityException
import orc.error.compiletime.typing.NoMatchingConstructorException
import orc.error.compiletime.typing.NoSuchMemberException

/** The type of a Java class, providing constructors and static members.
  *
  * @author dkitchin
  */
case class JavaClassType(val cl: Class[_], javaContext: Map[jvm.TypeVariable[_], Type] = Nil.toMap) extends CallableType with JavaType with StrictCallableType with HasMembersType {

  override def toString = "(class " + Option(cl.getCanonicalName).getOrElse(cl.getName) + ")"

  /* JVM class types do not yet have an implementation of join or meet.
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
      case JavaClassType(otherCl, _) => otherCl isAssignableFrom cl
      case _ => super.<(that)
    }
  }

  def call(typeArgs: List[Type], argTypes: List[Type]): Type = {
    // Static member access is via getField
    // Constructor call
    val formals = cl.getTypeParameters()
    if (formals.size != typeArgs.size) {
      throw new TypeArgumentArityException(formals.size, typeArgs.size)
    } else {
      val newJavaContext = javaContext ++ (formals zip typeArgs)
      def valid(ctor: jvm.Constructor[_]): Boolean = {
        val ctorFormals = ctor.getGenericParameterTypes()
        jvm.Modifier.isPublic(ctor.getModifiers()) &&
          ctorFormals.size == argTypes.size &&
          {
            val orcFormals = ctorFormals map { liftJavaType(_, newJavaContext) }
            (argTypes corresponds orcFormals) { _ < _ }
          }
      }
      if (cl.getConstructors() exists valid) {
        if (formals.isEmpty) {
          liftJavaType(cl)
        } else {
          liftJavaTypeOperator(cl).operate(typeArgs)
        }
      } else {
        throw new NoMatchingConstructorException()
      }
    }
  }

  def getMember(f: FieldType): Type = {
    typeOfMember(f.f, true, javaContext)
  }
}
