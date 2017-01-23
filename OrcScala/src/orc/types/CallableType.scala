//
// CallableType.scala -- Scala trait CallableType
// Project OrcScala
//
// Created by dkitchin on Nov 26, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing._

/** A callable type.
  *
  * @author dkitchin
  */
trait CallableType extends Type {
  def call(typeArgs: List[Type], argTypes: List[Type]): Type
}

/*
 * Indicates that this callable type is strict in its arguments.
 * If any argument has type Bot, the return type will also be Bot.
 */
trait StrictCallableType extends CallableType

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

/* Use case: no type arguments, two arguments */
trait BinaryCallableType extends SimpleCallableType {

  def call(t: Type, u: Type): Type

  def call(argTypes: List[Type]) = {
    argTypes match {
      case List(t, u) => call(t, u)
      case _ => throw new ArgumentArityException(2, argTypes.size)
    }
  }

}
