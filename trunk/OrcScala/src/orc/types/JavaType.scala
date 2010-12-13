//
// JavaType.scala -- Scala class/trait/object JavaType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Dec 13, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import java.lang.{reflect => java}
import orc.compile.typecheck.Typeloader._
import orc.error.compiletime.typing.NoSuchMemberException

/**
 * 
 *
 * @author dkitchin
 */
trait JavaType {
  self: Type => 

  val cl: Class[_]
  
  def findField(name: String, isStatic: Boolean): Option[java.Field] = {
    cl.getFields().toList find 
      { f => 
          f.getName().equals(name) &&
          java.Modifier.isPublic(f.getModifiers()) &&
          (java.Modifier.isStatic(f.getModifiers()) == isStatic)
      }
  }
  
  def findMethods(name: String, isStatic: Boolean): List[java.Method] = {
    cl.getMethods().toList filter 
      { m =>
          m.getName().equals(name) &&
          java.Modifier.isPublic(m.getModifiers()) &&
          (java.Modifier.isStatic(m.getModifiers()) == isStatic)
          !m.isVarArgs()
      }
  }
  
  def typeOfMember(name: String, isStatic: Boolean, javaContext: Map[java.TypeVariable[_], Type]): Type = {
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
      /*
       * FIXME: Will we need to pick the most specific method at the call point?
       *        We definitely can't pick it here; we don't know the call args yet.
       *        If so, creating an overloaded type here is the wrong solution.
       */ 
      case ms => {
        OverloadedType(ms map { liftJavaMethod(_, javaContext) })
      }
    }
  }
  
}