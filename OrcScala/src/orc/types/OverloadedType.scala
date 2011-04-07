//
// OverloadedType.scala -- Scala class OverloadedType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 30, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing.TypeException
import orc.error.compiletime.typing.OverloadedTypeException

/**
 * 
 * Ad-hoc polymorphic callable types.
 * 
 * For simplicity and modularity of implementation, type inference 
 * and type meets and joins in the core type system will not generate 
 * or interact with overloaded types; they use the default meet and join.
 * 
 * Thus, explicit type annotations will sometimes be required where 
 * one might expect the type system to find a more informative result.
 * 
 * TODO: Refine these types to play nice with meets and joins; this may
 * require modifications to other types, especially FunctionType.
 * 
 * These types cannot be written by the programmer. They can only be
 * introduced by sites.
 *
 * @author dkitchin
 */
case class OverloadedType(alternatives: List[CallableType]) extends CallableType {
  
  override def toString = alternatives.mkString("(", " /\\ ", ")")
  
  override def <(that: Type): Boolean = {
    for (t <- alternatives) {
      if (t < that) { 
        return true 
      }
    }
    // otherwise
    super.<(that)
  }
  
  def call(typeArgs: List[Type], argTypes: List[Type]): Type = {
    var failure = new OverloadedTypeException()
    for (t <- alternatives) {
      try {
        return t.call(typeArgs, argTypes)
      }
      catch {
        case e: TypeException => {
          failure = failure.addAlternative(t, e)
        }
      }
    }
    // otherwise
    throw failure
  }
  
}
