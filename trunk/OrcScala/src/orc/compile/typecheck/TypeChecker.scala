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

package orc.compile.typecheck

import orc.oil.nameless.{ Type => _, Top => _, Bot => _, FunctionType => _, _ }
import orc.types._
import orc.error.compiletime.typing._
import orc.types.TypeConversions._

object TypeChecker {

  def typeSynth(expr: Expression, context: List[Type], typeContext: List[Type]): Type = {
    expr match {
      case Stop() => Bot
      case Hole(_,_) => Bot
      case Call(target, args, typeArgs) => {
        val callee = typeSynth(target, context, typeContext)
        callee match {
          case ArrowType(typeFormalArity, argTypes, returnType) => {
            if (argTypes.size != args.size) throw new ArgumentArityException(argTypes.size, args.size) //FIXME: provide source location
            if (typeFormalArity != typeArgs.size) throw new TypeArityException(typeFormalArity, typeArgs.size) //FIXME: provide source location
            for ((p, a) <- argTypes zip args) typeCheck(a, p, context, typeContext)
            returnType
          }
          case t => throw new UncallableTypeException(t) //FIXME: provide source location
        }
      }
      case Parallel(left, right) => typeSynth(left, context, typeContext) join typeSynth(right, context, typeContext)
      case Sequence(left, right) => typeSynth(right, typeSynth(left, context, typeContext) :: context, typeContext)
      case Prune(left, right) => typeSynth(left, typeSynth(right, context, typeContext) :: context, typeContext)
      case Otherwise(left, right) => typeSynth(left, context, typeContext) join typeSynth(right, context, typeContext)
      case DeclareDefs(_, defs, body) => {
        val defTypes = for (d <- defs) yield {
          //FIXME: Handle inference of arg types and/or return type
          ArrowType(d.typeFormalArity, d.argTypes.get, d.returnType.get)
        }
        for (d <- defs) typeCheckDef(d, defTypes.reverse ::: context, typeContext)
        typeSynth(body, defTypes.reverse ::: context, typeContext)
      }
      case HasType(body, expectedType) => typeCheck(body, expectedType, context, typeContext); expectedType
      case DeclareType(t, body) => typeSynth(body, context, t :: typeContext)
      case Constant(value) => Top //FIXME
      case Variable(index) => context(index)
    }
  }

  def typeCheckDef(defn: Def, context: List[Type], typeContext: List[Type]) {
    val Def(typeFormalArity, arity, body, argTypes, returnType) = defn
    //FIXME: Handle inference of arg types and/or return type
    typeCheck(body, returnType.get, argTypes.get.reverse ::: context, typeContext)
  }

  def typeCheck(expr: Expression, checkType: Type, context: List[Type], typeContext: List[Type]) {
    expr match {
      case Parallel(left, right) => typeCheck(left, checkType, context, typeContext); typeCheck(right, checkType, context, typeContext)
      case Sequence(left, right) => typeCheck(right, checkType, typeSynth(left, context, typeContext) :: context, typeContext)
      case Prune(left, right) => typeCheck(left, checkType, typeSynth(right, context, typeContext) :: context, typeContext)
      case Otherwise(left, right) => typeCheck(left, checkType, context, typeContext); typeCheck(right, checkType, context, typeContext)
      case DeclareDefs(_, defs, body) => {
        //FIXME: Handle inference of arg types and/or return type
        val defTypes = for (d <- defs) yield ArrowType(d.typeFormalArity, d.argTypes.get, d.returnType.get)
        for (d <- defs) typeCheckDef(d, defTypes.reverse ::: context, typeContext)
        typeCheck(body, checkType, defTypes.reverse ::: context, typeContext)
      }
      case e => typeSynth(e, context, typeContext) assertSubtype checkType
    }
  }

}
