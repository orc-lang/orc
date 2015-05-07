//
// FunctionType.scala -- Scala class FunctionType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Nov 19, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

import orc.error.compiletime.typing.ArgumentArityException
import orc.error.compiletime.typing.TypeArgumentArityException

/**
 * 
 * The semantic type of functions.
 *
 * @author dkitchin
 */

case class FunctionType(val typeFormals: List[TypeVariable], val argTypes: List[Type], val returnType: Type) extends CallableType {
  
  val arity = argTypes.size
  val typeArity = typeFormals.size
    
  override def toString = "lambda" + typeFormals.mkString("[",", ","]") + argTypes.mkString("(",", ",")") + " :: " + returnType
  
  override def join(that: Type): Type = that match {
    case FunctionType(thatTypeFormals, thatArgTypes, thatReturnType) if (sameShape(that)) => {
      FunctionType(thatTypeFormals, argTypes meet thatArgTypes, returnType join thatReturnType)
    }
    case _ => super.join(that)
  }
  
  override def meet(that: Type): Type = that match {
    case FunctionType(thatTypeFormals, thatArgTypes, thatReturnType) if (sameShape(that)) => {
      FunctionType(thatTypeFormals, argTypes join thatArgTypes, returnType meet thatReturnType)
    }
    case _ => super.meet(that)
  }
  
  override def <(that: Type): Boolean = {
    that match {
      case f@ FunctionType(thatTypeFormals, thatArgTypes, thatReturnType) if (this sameShape that) => {
        val (FunctionType(_, thisNewArgTypes, thisNewReturnType),
             FunctionType(_, thatNewArgTypes, thatNewReturnType)) = this shareFormals f
        (thatNewArgTypes < thisNewArgTypes)  &&  (thisNewReturnType < thatNewReturnType)
      }
      case _ => super.<(that)
    }
  }
  
  
  override def subst(sigma: Map[TypeVariable, Type]): Type = {
    assert(typeFormals forall { x => !(sigma contains x) })
    FunctionType(typeFormals, argTypes map { _ subst sigma }, returnType subst sigma)
  }
  
  
  def sameShape(that: Type): Boolean = that match {
    case FunctionType(thatTypeFormals, thatArgTypes, thatReturnType) => {
      (typeFormals.size == thatTypeFormals.size)  &&  (argTypes.size == thatArgTypes.size)
    }
    case _ => false
  }
  
    /**
   * 
   * Given two function types,
   * [X] S -> R
   * [Y] T -> U
   * 
   * return a new pair of function types which share a common
   * set of type formals:
   * [Z] [Z/X]S -> [Z/X]R
   * [Z] [Z/Y]T -> [Z/Y]U
   * 
   */
  def shareFormals(that: FunctionType): (FunctionType, FunctionType) = {
    assert(this.typeArity == that.typeArity) 
    val sharedFormals = typeFormals map { x => new TypeVariable(x) }
    val newThis = ( this subst (sharedFormals, this.typeFormals) ).asInstanceOf[FunctionType]
    val newThat = ( that subst (sharedFormals, that.typeFormals) ).asInstanceOf[FunctionType]
    (newThis, newThat)
  }
  
  
  /* A function type may be treated as a callable type.
   * When performing type inference, we instead treat
   * function types structurally, to improve inference
   * accuracy in some cases. 
   */
  def call(callTypeArgs: List[Type], callArgTypes: List[Type]): Type = {
    if (typeFormals.size != callTypeArgs.size) {
      throw new TypeArgumentArityException(typeFormals.size, callTypeArgs.size)
    }
    if (argTypes.size != callArgTypes.size) {
      throw new ArgumentArityException(argTypes.size, callArgTypes.size)
    }
    val instantiatedArgTypes = argTypes map { _ subst (callTypeArgs, typeFormals) }
    val instantiatedReturnType = returnType subst (callTypeArgs, typeFormals)
    for ( (t, u) <- (callArgTypes zip instantiatedArgTypes) ) {
      t assertSubtype u
    }
    instantiatedReturnType
  }
  
}

/* Use cases */
object SimpleFunctionType {
  
  def apply(returnType: Type) = {
    FunctionType(Nil, Nil, returnType)
  }
  
  def apply(argType: Type, returnType: Type) = {
    FunctionType(Nil, List(argType), returnType)
  }
  
  def apply(argType1: Type, argType2: Type, returnType: Type) = {
    FunctionType(Nil, List(argType1, argType2), returnType)
  }
  
  def apply(argTypes: List[Type], returnType: Type) = {
    FunctionType(Nil, argTypes, returnType)
  }
  
}
