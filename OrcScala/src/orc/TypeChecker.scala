//
// TypeChecker.scala -- Scala object TypeChecker
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
package orc

object TypeChecker {
	import oil._
	import types._

	def typeSynth(expr : Expression, context : List[Type]) : Type = {
		expr match {
			case Stop() => Bot()
			case Call(target, args) => {
				val callee = typeSynth(target, context)
				callee match {
					case ArrowType(paramTypes, returnType) => {
						if (paramTypes.size != args.size) throw new ArgumentArityException() //FIXME: provide source location
						for ((p, a) <- paramTypes zip args) typeCheck(a, p, context)  
						returnType
					}
					case _ => throw new UncallableTypeException() //FIXME: provide source location
				}
			}
			case Parallel(left, right) => typeSynth(left, context) join typeSynth(right, context)   
			case Sequence(left, right) => typeSynth(right, typeSynth(left, context)::context)
			case Prune(left, right) => typeSynth(left, typeSynth(right, context)::context)
			case Cascade(left, right) => typeSynth(left, context) join typeSynth(right, context)
			case DeclareDefs(defs, body) => {
				val defTypes = for (d <- defs) yield ArrowType(d.paramTypes, d.returnType)
				for (d <- defs) typeCheckDef(d, defTypes.reverse:::context)
				typeSynth(body, defTypes.reverse:::context)
			}
			case Constant(value) => Top() //FIXME
			case Variable(index) => context(index)
		}
	}

	def typeCheckDef(defn : Def, context : List[Type]) = {
		val Def(arity, body, paramTypes, returnType) = defn
		typeCheck(body, returnType, paramTypes.reverse ::: context)
	}
	
	def typeCheck(expr : Expression, checkType : Type, context : List[Type]) {
		expr match {
			case Parallel(left, right) => typeCheck(left, checkType, context) ; typeCheck(right, checkType, context)
			case Sequence(left, right) => typeCheck(right, checkType, typeSynth(left, context)::context)
			case Prune(left, right) => typeCheck(left, checkType, typeSynth(right, context)::context)
			case Cascade(left, right) => typeCheck(left, checkType, context) ; typeCheck(right, checkType, context)
			case DeclareDefs(defs, body) => {
				val defTypes = for (d <- defs) yield ArrowType(d.paramTypes, d.returnType)
				for (d <- defs) typeCheckDef(d, defTypes.reverse:::context)
				typeCheck(body, checkType, defTypes.reverse:::context)
			}
			case e => typeSynth(e, context) assertSubtype checkType 
		}
	}

}