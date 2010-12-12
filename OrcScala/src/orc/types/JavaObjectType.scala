//
// JvmObjectType.scala -- Scala class JvmObjectType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 29, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.NotYetImplementedException
import java.lang.{reflect => java}

/**
 *
 * The type of a JVM object.
 *
 * @author dkitchin
 */
case class JavaObjectType(C: Class[_], javaContext: Map[java.TypeVariable[_], Type] = Nil.toMap) extends SimpleCallableType {

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
      case JavaObjectType(otherC, _) => otherC isAssignableFrom C
      case _ => super.<(that)
    }
  }
  
  def call(argTypes: List[Type]): Type = throw new NotYetImplementedException()
  
}
