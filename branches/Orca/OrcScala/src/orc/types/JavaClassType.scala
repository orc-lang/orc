//
// JvmObjectType.scala -- Scala class JvmObjectType
// Project OrcScala
//
// $Id: JavaClassType.scala 2773 2011-04-20 01:12:36Z jthywissen $
//
// Created by dkitchin on Nov 29, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.NotYetImplementedException
import orc.error.compiletime.typing.TypeArgumentArityException
import orc.error.compiletime.typing.NoMatchingConstructorException
import java.lang.{reflect => jvm}
import orc.compile.typecheck.Typeloader._

/**
 *
 * The type of a Java class, providing constructors and static members.
 *
 * @author dkitchin
 */
case class JavaClassType(val cl: Class[_], javaContext: Map[jvm.TypeVariable[_], Type] = Nil.toMap) extends CallableType with JavaType with StrictType {

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
    argTypes match {
      // Static member access
      case List(FieldType(name)) => {
        if (typeArgs.size > 0) {
          throw new TypeArgumentArityException(0, typeArgs.size)
        }
        typeOfMember(name, true, javaContext)
      }
      // Constructor call
      case _ => {
        val formals = cl.getTypeParameters()
        if (formals.size != typeArgs.size) {
          throw new TypeArgumentArityException(formals.size, typeArgs.size)
        }
        else {
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
            }
            else {
              liftJavaTypeOperator(cl).operate(typeArgs)
            }
          }
          else {
            throw new NoMatchingConstructorException()
          }
        }
      }
    } 
  }
  
}
