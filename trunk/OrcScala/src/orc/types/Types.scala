//
// Types.scala -- Scala package orc.types
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 24, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

abstract class Type {
	def join(that: Type) : Type
	def meet(that: Type) : Type
	def assertSubtype(that: Type)
}
case class Top extends Type {
	def join(that: Type) : Type = this
	def meet(that: Type) : Type = that
	def assertSubtype(that: Type) {
		that match {
			case Top() => { }
			case _ => throw new SubtypeFailureException(this, that) //FIXME: Supply source location 
		}
	}
}
case class Bot extends Type {
	def join(that: Type) : Type = that
	def meet(that: Type) : Type = this
	def assertSubtype(that: Type) { }
}

case class ArrowType(paramTypes: List[Type], returnType: Type) extends Type {
	def join(that: Type) : Type = that match {
		case ArrowType(thatParamTypes, thatReturnType) if (paramTypes.size == thatParamTypes.size) => {
			val combinedParamTypes = for ((t1, t2) <- paramTypes zip thatParamTypes) yield t1 meet t2
			ArrowType(combinedParamTypes, returnType join thatReturnType)
		}
		case _ => Top()
	}
	def meet(that: Type) : Type = that match {
		case ArrowType(thatParamTypes, thatReturnType) if (paramTypes.size == thatParamTypes.size) => {
			val combinedParamTypes = for ((t1, t2) <- paramTypes zip thatParamTypes) yield t1 join t2
			ArrowType(combinedParamTypes, returnType meet thatReturnType)
		}
		case _ => Bot()
	}
	def assertSubtype(that: Type) {
		that match {
			case ArrowType(thatParamTypes, thatReturnType) if (paramTypes.size == thatParamTypes.size) => {
				val combinedParamTypes = for ((t1, t2) <- paramTypes zip thatParamTypes) yield t1 meet t2
				returnType assertSubtype thatReturnType
			}
			case _ => throw new SubtypeFailureException(this, that) //FIXME: Supply source location
		}
	}
}
