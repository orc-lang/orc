//
// CallableType.scala -- Scala class/trait/object CallableType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing._

/**
 * An opaquely callable type.
 * 
 * Note that FunctionType does not extend this trait, since its
 * type content is entirely structural; in particular, the typechecker
 * will check a call against the stated argument types of the function
 * and yield the return type, rather than synthesizing the argument
 * types and procedurally computing the return type as is done here. 
 * 
 * @author dkitchin
 */
trait CallableType extends Type {
  def call(typeArgs: List[Type], argTypes: List[Type]): Type
}

/* Use case: no type arguments */
trait SimpleCallableType extends CallableType {
  
  def call(argTypes: List[Type]): Type
  
  def call(typeArgs: List[Type], argTypes: List[Type]): Type = {
    typeArgs match {
      case Nil => call(argTypes)
      case _ => throw new TypeArgumentArityException(0, typeArgs.size)
    }
  }
  
}


/* Use case: no type arguments, one argument */
trait UnaryCallableType extends SimpleCallableType {
  
  def call(argType: Type): Type
  
  def call(argTypes: List[Type]) = {
    argTypes match {
      case List(t) => call(t)
      case _ => throw new ArgumentArityException(0, argTypes.size)
    }
  }
  
}