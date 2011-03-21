//
// CallableType.scala -- Scala trait CallableType
// Project OrcScala
//
// $Id: CallableType.scala 2228 2010-12-07 19:13:50Z jthywissen $
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
 * A callable type.
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
      case _ => throw new ArgumentArityException(1, argTypes.size)
    }
  }
  
}
