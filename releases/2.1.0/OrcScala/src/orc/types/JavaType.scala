//
// JavaType.scala -- Scala trait JavaType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Dec 13, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import java.lang.{ reflect => jvm }
import orc.compile.typecheck.Typeloader._
import orc.error.compiletime.typing.NoSuchMemberException
import orc.values.sites.OrcJavaCompatibility
import java.lang.reflect.Modifier

/**
  *
  * @author dkitchin
  */
trait JavaType {
  self: Type =>

  val cl: Class[_]

  def findField(name: String, isStatic: Boolean): Option[jvm.Field] = {
    cl.getFields().toList find
      { f =>
        f.getName().equals(name) &&
          jvm.Modifier.isPublic(f.getModifiers()) &&
          (jvm.Modifier.isStatic(f.getModifiers()) == isStatic)
      }
  }

  def findMethods(name: String, isStatic: Boolean): List[jvm.Method] = {
    OrcJavaCompatibility.getAccessibleMethods(cl).toList filter
      { m =>
        m.getName().equals(name) &&
          jvm.Modifier.isPublic(m.getModifiers()) &&
          (jvm.Modifier.isStatic(m.getModifiers()) == isStatic) &&
          !m.isVarArgs()
      }
  }

  def typeOfMember(name: String, isStatic: Boolean, javaContext: Map[jvm.TypeVariable[_], Type]): Type = {
    findMethods(name, isStatic) match {
      // No matching methods; maybe it's a field?
      case Nil => {
        findField(name, isStatic) match {
          case Some(f) => liftJavaField(f, javaContext)
          case None => throw new NoSuchMemberException(this, name)
        }
      }
      case List(m) => {
        liftJavaMethod(m, javaContext)
      }
      case ms => {
        /* Since OverloadedType.call picks the first alternative that type checks,
         * sort methods in most-to-least specific order. See JLS § 15.12.2.5
         */
        val mss = ms.sortWith({ (l, r) =>
          l.getParameterTypes().length < r.getParameterTypes().length ||
          OrcJavaCompatibility.isEqOrMoreSpecific(l, r) ||
          (!Modifier.isAbstract(l.getModifiers()) && Modifier.isAbstract(r.getModifiers())) ||
          OrcJavaCompatibility.isJavaSubtypeOf(l.getReturnType(),r.getReturnType())
        })
        OverloadedType(mss map { liftJavaMethod(_, javaContext) })
      }
    }
  }

}
