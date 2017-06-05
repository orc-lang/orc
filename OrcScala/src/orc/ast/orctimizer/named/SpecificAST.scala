//
// SpecificAST.scala -- Scala class SpecificAST
// Project OrcScala
//
// Created by amp on Mar 17, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.orctimizer.named

import orc.ast.PrecomputeHashcode
import scala.collection.generic.CanBuildFrom

// FIXME: This cannot distinguish identical subexpressions. For instance, in 1 | 1 the two "1"s are not different.
case class SpecificAST[+T <: NamedAST](ast: T, path: List[NamedAST]) extends PrecomputeHashcode {
  // Just check that ast is a member of the first element of path.
  if (false)
    (ast :: path) match {
      case b :: a :: _ =>
        assert(a.subtrees.toSet contains b, s"Path ${path.map(SpecificAST.shortString).mkString("[", ", ", "]")} does not contain a parent of $ast.\n$b === is not a subtree of ===\n$a\n${a.subtrees}")
      case Seq(_) => true
    }
  /* This complete check turns out to be a major performance cost
  (ast :: path).tails foreach {
    case b :: a :: _ =>
      assert(a.subtrees.toSet contains b, s"Path ${path.map(SpecificAST.shortString).mkString("[", ", ", "]")} does not contain a parent of $ast.\n$b === is not a subtree of ===\n$a\n${a.subtrees}")
    case Seq(_) => true
    case Seq() => true
  }
  */

  def subtreePath = ast :: path

  def subtree[E <: NamedAST](e: E): SpecificAST[E] =
    SpecificAST(e, subtreePath)

  def subtrees = ast.subtrees.map(subtree)

  override def toString() = {
    s"$productPrefix($ast, ${path.take(4).map(SpecificAST.shortString).mkString("[", ", ", ", ...]")})"
  }
}

object SpecificAST {
  import scala.language.implicitConversions
  private def shortString(o: AnyRef) = s"'${o.toString().replace('\n', ' ').take(30)}'"

  implicit def SpecificAST2AST[T <: NamedAST](l: SpecificAST[T]): T = l.ast
}

trait SpecificASTTransform {
  def apply(e: Expression) = transformExpression(SpecificAST(e, Nil))

  def onExpression: PartialFunction[SpecificAST[Expression], Expression] = EmptyFunction

  def onArgument: PartialFunction[SpecificAST[Argument], Argument] = EmptyFunction

  def onType: PartialFunction[SpecificAST[Type], Type] = EmptyFunction

  def onCallable: PartialFunction[SpecificAST[Callable], Callable] = EmptyFunction

  def onFieldValue: PartialFunction[SpecificAST[FieldValue], FieldValue] = EmptyFunction

  def transformType(t: SpecificAST[Type]): SpecificAST[Type] = {
    order[Type](onType, e => {
      def transformTypeList(elements: List[Type]) = elements.map((f: Type) => transformType(e.subtree(f)).ast)
      e.ast match {
        case TupleType(elements) => TupleType(transformTypeList(elements))
        case FunctionType(formals, argTypes, returnType) => FunctionType(formals, transformTypeList(argTypes), transformType(e.subtree(returnType)))
        case TypeApplication(tycon, typeactuals) => TypeApplication(transformType(e.subtree(tycon)), transformTypeList(typeactuals))
        case AssertedType(assertedType) => AssertedType(transformType(e.subtree(assertedType)))
        case TypeAbstraction(typeformals, t) => TypeAbstraction(typeformals, transformType(e.subtree(t)))
        case RecordType(entries) => {
          RecordType(entries.mapValues((f: Type) => transformType(e.subtree(f)).ast).view.force)
        }
        case StructuralType(entries) => {
          StructuralType(entries.mapValues((f: Type) => transformType(e.subtree(f)).ast).view.force)
        }
        case NominalType(t) => NominalType(transformType(e.subtree(t)))
        case IntersectionType(a, b) => IntersectionType(transformType(e.subtree(a)), transformType(e.subtree(b)))
        case UnionType(a, b) => UnionType(transformType(e.subtree(a)), transformType(e.subtree(b)))
        case VariantType(self, typeformals, variants) => {
          VariantType(self, typeformals, variants.map({ case (n, as) =>  (n, transformTypeList(as))}))
        }
        case Bot() | ClassType(_) | ImportedType(_) | Top() | UnboundTypevar(_) => e
        case _: BoundTypevar => e
      }
    })(t)
  }

  def transformCallable(d: SpecificAST[Callable]): SpecificAST[Callable] = {
    order[Callable](onCallable, e => {
      val Callable(name, formals, body, typeformals, argtypes, returntype) = e.ast
      val newbody = transformExpression(e.subtree(body))
      val newargtypes = argtypes map { _ map { f => transformType(e.subtree(f)).ast } }
      val newreturntype = returntype map { f => transformType(e.subtree(f)).ast }
      e.ast.copy(name, formals, newbody, typeformals, newargtypes, newreturntype)
    })(d)
  }

  def transformExpression(e: SpecificAST[Expression]): SpecificAST[Expression] = {
    order[Expression](onExpression, e => e.ast match {
      case CallDef(target, args, typeargs) => {
        val newtarget = transformArgument(e.subtree(target)).ast
        val newargs = args map { f => transformArgument(e.subtree(f)).ast }
        val newtypeargs = typeargs map { _ map { f => transformType(e.subtree(f)).ast } }
        CallDef(newtarget, newargs, newtypeargs)
      }
      case CallSite(target, args, typeargs) => {
        val newtarget = transformArgument(e.subtree(target)).ast
        val newargs = args map { f => transformArgument(e.subtree(f)).ast }
        val newtypeargs = typeargs map { _ map { f => transformType(e.subtree(f)).ast } }
        CallSite(newtarget, newargs, newtypeargs)
      }
      case left Parallel right =>
        Parallel(transformExpression(e.subtree(left)), transformExpression(e.subtree(right)))
      case Branch(left, x, right) =>
        Branch(transformExpression(e.subtree(left)), x, transformExpression(e.subtree(right)))
      case Trim(f) =>
        Trim(transformExpression(e.subtree(f)))
      case Force(xs, vs, b, f) => {
        val newvs = vs map { f => transformArgument(e.subtree(f)).ast }
        Force(xs, newvs, b, transformExpression(e.subtree(f)))
      }
      case IfDef(v, left, right) =>
        IfDef(transformArgument(e.subtree(v)), transformExpression(e.subtree(left)), transformExpression(e.subtree(right)))
      case Future(f) =>
        Future(transformExpression(e.subtree(f)))
      case left Otherwise right =>
        Otherwise(transformExpression(e.subtree(left)), transformExpression(e.subtree(right)))
      case DeclareCallables(defs, body) => {
        val newdefs = defs map { f => transformCallable(e.subtree(f)).ast }
        DeclareCallables(newdefs, transformExpression(transformExpression(e.subtree(body))))
      }
      case FieldAccess(o, f) =>
        FieldAccess(transformArgument(e.subtree(o)), f)
      case New(self, st, members, ot) => {
        val newmembers = members.mapValues(f => transformFieldValue(e.subtree(f)).ast).view.force
        New(self, st map { t => transformType(e.subtree(t)).ast }, newmembers, ot map { t => transformType(e.subtree(t)).ast })
      }
      case HasType(body, expectedType) =>
        HasType(transformExpression(e.subtree(body)), transformType(e.subtree(expectedType)))
      case DeclareType(u, t, body) =>
        DeclareType(u, transformType(e.subtree(t)), transformExpression(e.subtree(body)))
      case Stop() =>
        Stop()
      case a: Argument =>
        transformArgument(SpecificAST(a, e.path))
    })(e)
  }

  def transformArgument(a: SpecificAST[Argument]): SpecificAST[Argument] = {
    order[Argument](onArgument, e => e.ast match {
      case v: BoundVar => e
      case UnboundVar(_) => e
      case Constant(_) => e
    })(a)
  }

  def transformFieldValue(d: SpecificAST[FieldValue]): SpecificAST[FieldValue] = {
    order[FieldValue](EmptyFunction, e => e.ast match {
      case FieldFuture(f) => FieldFuture(transformExpression(e.subtree(f)))
      case FieldArgument(f) => FieldArgument(transformArgument(e.subtree(f)))
    })(d)
  }

  def order[E <: NamedAST](pf: PartialFunction[SpecificAST[E], E], descend: SpecificAST[E] => E)(e: SpecificAST[E]): SpecificAST[E] = {
    val e1 = SpecificAST(e ->> descend(e), e.path)
    val o = pf.lift(e1)
    SpecificAST(e ->> o.map(eo => if(e1.ast == eo) e1.ast else eo).getOrElse(e1.ast), e.path)
  }
}
