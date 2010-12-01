//
// BufferType.scala -- Scala class/trait/object BufferType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Dec 1, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.state

import orc.types._
import orc.error.compiletime.typing._

/**
 * 
 *
 * @author dkitchin
 */

object BufferSiteType extends CallableType { 
  def call(typeArgs: List[Type], argTypes: List[Type]): Type = {
    if (!argTypes.isEmpty) { throw new ArgumentArityException(0, argTypes.size) }
    typeArgs match {
      case List(t) => BufferType(t)
      case _ => throw new TypeArgumentArityException(1, typeArgs.size)
    }
  }
}

object BufferType extends SimpleTypeConstructor("Buffer", Invariant) {
  override def apply(ts: Type*): Type = {
    ts.toList match {
      case List(t) => new BufferInstanceType(t)
    }
  }
}

class BufferInstanceType(t: Type) extends TypeInstance(BufferType, List(t)) with TypeWithMembers {
  def getMember(member: String) = 
    member match {
      case "get" => Some( SimpleFunctionType(t) )
      case "put" => Some( SimpleFunctionType(t, SignalType) )
      case _ => None
    }
}



