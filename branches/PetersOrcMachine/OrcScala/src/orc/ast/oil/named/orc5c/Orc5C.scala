//
// Named.scala -- Named representation of OIL syntax
// Project OrcScala
//
// $Id: Named.scala 3197 2013-04-24 22:17:43Z jthywissen $
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.named.orc5c

import orc.ast.oil._
import orc.ast.AST
import orc.ast.hasOptionalVariableName

trait PrecomputeHashcode { self: Product => 
  override val hashCode: Int = scala.runtime.ScalaRunTime._hashCode(this)
}

sealed abstract class Orc5CAST extends AST {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()

  override val subtrees: Iterable[Orc5CAST] = this match {
    case Call(target, args, typeargs) => target :: (args ::: typeargs.toList.flatten)
    case left || right => List(left, right)
    case Sequence(left, x, right) => List(left, x, right)
    case LateBind(left, x, right) => List(left, x, right)
    case Limit(f) => List(f)
    case left ow right => List(left, right)
    case DeclareDefs(defs, body) => defs ::: List(body)
    case VtimeZone(timeOrder, body) => List(timeOrder, body)
    case HasType(body, expectedType) => List(body, expectedType)
    case DeclareType(u, t, body) => List(u, t, body)
    case Def(f, formals, body, typeformals, argtypes, returntype) => {
      f :: (formals ::: (List(body) ::: typeformals ::: argtypes.toList.flatten ::: returntype.toList))
    }
    case TupleType(elements) => elements
    case FunctionType(_, argTypes, returnType) => argTypes :+ returnType
    case TypeApplication(tycon, typeactuals) => tycon :: typeactuals
    case AssertedType(assertedType) => List(assertedType)
    case TypeAbstraction(typeformals, t) => typeformals ::: List(t)
    case RecordType(entries) => entries.values
    case VariantType(self, typeformals, variants) => {
      self :: typeformals ::: (for ((_, variant) <- variants; t <- variant) yield t)
    }
    case Constant(_) | UnboundVar(_) | Stop() => Nil
    case Bot() | ClassType(_) | ImportedType(_) | Top() | UnboundTypevar(_) => Nil
    case _: BoundVar | _: BoundTypevar => Nil
    case undef => throw new scala.MatchError(undef.getClass.getCanonicalName + " not matched in Orc5CAST.subtrees")
  }
}

sealed abstract class Expression
  extends Orc5CAST
  with Orc5CInfixCombinators
  with Substitution[Expression]
  with hasVars 

case class Stop() extends Expression with PrecomputeHashcode
case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression with PrecomputeHashcode
case class Parallel(left: Expression, right: Expression) extends Expression with PrecomputeHashcode
case class Sequence(left: Expression, x: BoundVar, right: Expression) extends Expression with PrecomputeHashcode
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
case class LateBind(left: Expression, x: BoundVar, right: Expression) extends Expression with PrecomputeHashcode
  with hasOptionalVariableName { transferOptionalVariableName(x, this) }
case class Limit(expr: Expression) extends Expression with PrecomputeHashcode
case class Otherwise(left: Expression, right: Expression) extends Expression with PrecomputeHashcode
case class DeclareDefs(defs: List[Def], body: Expression) extends Expression with PrecomputeHashcode
case class DeclareType(name: BoundTypevar, t: Type, body: Expression) extends Expression with PrecomputeHashcode
  with hasOptionalVariableName { transferOptionalVariableName(name, this) }
case class HasType(body: Expression, expectedType: Type) extends Expression with PrecomputeHashcode
case class VtimeZone(timeOrder: Argument, body: Expression) extends Expression with PrecomputeHashcode

sealed abstract class Argument extends Expression
case class Constant(value: AnyRef) extends Argument
trait Var extends Argument with hasOptionalVariableName
case class UnboundVar(name: String) extends Var {
  optionalVariableName = Some(name)
}
class BoundVar(optionalName: Option[String] = None) extends Var with hasOptionalVariableName {
  optionalVariableName = optionalName match {
    case Some(n) => Some(n)
    case None =>
      Some(BoundVar.getNextVariableName())
  }

  def productIterator = optionalVariableName.toList.iterator
}

object BoundVar {
  private var nextVar: Int = 0
  def getNextVariableName() = synchronized {
    nextVar += 1
    s"`v$nextVar"
  }
}

sealed case class Def(name: BoundVar, formals: List[BoundVar], body: Expression, typeformals: List[BoundTypevar], argtypes: Option[List[Type]], returntype: Option[Type])
  extends Orc5CAST
  with hasOptionalVariableName
  with PrecomputeHashcode {
  transferOptionalVariableName(name, this)
  def copy(name: BoundVar = name,
    formals: List[BoundVar] = formals,
    body: Expression = body,
    typeformals: List[BoundTypevar] = typeformals,
    argtypes: Option[List[Type]] = argtypes,
    returntype: Option[Type] = returntype) = {
    this ->> Def(name, formals, body, typeformals, argtypes, returntype)
  }
}

sealed abstract class Type
  extends Orc5CAST {
}
case class Top() extends Type with PrecomputeHashcode
case class Bot() extends Type with PrecomputeHashcode
case class TupleType(elements: List[Type]) extends Type with PrecomputeHashcode
case class RecordType(entries: Map[String, Type]) extends Type with PrecomputeHashcode
case class TypeApplication(tycon: Type, typeactuals: List[Type]) extends Type with PrecomputeHashcode
case class AssertedType(assertedType: Type) extends Type with PrecomputeHashcode
case class FunctionType(typeformals: List[BoundTypevar], argtypes: List[Type], returntype: Type) extends Type with PrecomputeHashcode
case class TypeAbstraction(typeformals: List[BoundTypevar], t: Type) extends Type with PrecomputeHashcode
case class ImportedType(classname: String) extends Type with PrecomputeHashcode
case class ClassType(classname: String) extends Type with PrecomputeHashcode
case class VariantType(self: BoundTypevar, typeformals: List[BoundTypevar], variants: List[(String, List[Type])]) extends Type with PrecomputeHashcode

trait Typevar extends Type with hasOptionalVariableName
case class UnboundTypevar(name: String) extends Typevar {
  optionalVariableName = Some(name)
}
class BoundTypevar(optionalName: Option[String] = None) extends Typevar with hasOptionalVariableName {

  optionalVariableName = optionalName

  def productIterator = optionalVariableName.toList.iterator
}

