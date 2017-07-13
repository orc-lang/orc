//
// NamedASTTransform.scala -- Scala traits NamedASTFunction and NamedASTTransform and object EmptyFunction
// Project OrcScala
//
// Created by dkitchin on Jul 12, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named

import scala.language.reflectiveCalls
import orc.compile.Logger

/** @author dkitchin
  */
trait NamedASTFunction {
  def apply(a: Argument): Argument
  def apply(e: Expression): Expression
  def apply(t: Type): Type
  def apply(d: Callable): Callable

  def apply(ast: NamedAST): NamedAST = {
    ast match {
      case a: Argument => this(a)
      case e: Expression => this(e)
      case t: Type => this(t)
      case d: Callable => this(d)
    }
  }

  def andThen(g: NamedASTFunction): NamedASTFunction = {
    val f = this
    new NamedASTFunction {
      def apply(a: Argument): Argument = g(f(a))
      def apply(e: Expression): Expression = g(f(e))
      def apply(t: Type): Type = g(f(t))
      def apply(d: Callable): Callable = g(f(d))
    }
  }

}

object EmptyFunction extends PartialFunction[Any, Nothing] {
  def isDefinedAt(x: Any): Boolean = false
  def apply(x: Any): Nothing = throw new AssertionError("EmptyFunction is undefined for all inputs.")
}

trait NamedASTTransform extends NamedASTFunction {
  def apply(a: Argument): Argument = transform(a, Nil)
  def apply(e: Expression): Expression = transform(e, Nil, Nil)
  def apply(t: Type): Type = transform(t, Nil)
  def apply(d: Callable): Callable = transform(d, Nil, Nil)

  def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]): PartialFunction[Expression, Expression] = EmptyFunction

  def onArgument(context: List[BoundVar]): PartialFunction[Argument, Argument] = EmptyFunction

  def onType(typecontext: List[BoundTypevar]): PartialFunction[Type, Type] = EmptyFunction

  def onCallable(context: List[BoundVar], typecontext: List[BoundTypevar]): PartialFunction[Callable, Callable] = EmptyFunction

  def recurseWithContext(context: List[BoundVar], typecontext: List[BoundTypevar]) =
    new NamedASTFunction {
      def apply(a: Argument) = transform(a, context)
      def apply(e: Expression) = transform(e, context, typecontext)
      def apply(t: Type) = transform(t, typecontext)
      def apply(d: Callable) = transform(d, context, typecontext)
    }

  def transform(a: Argument, context: List[BoundVar]): Argument = {
    val pf = onArgument(context)
    if (pf isDefinedAt a) {
      a -> pf
    } else a
  }

  def transform(e: Expression, context: List[BoundVar], typecontext: List[BoundTypevar]): Expression = {
    val pf = onExpression(context, typecontext)
    if (pf isDefinedAt e) {
      e -> pf
    } else {
      val recurse = recurseWithContext(context, typecontext)
      e -> {
        case Stop() => Stop()
        case a: Argument => recurse(a)
        case Call(target, args, typeargs) => {
          val newtarget = recurse(target)
          val newargs = args map { recurse(_) }
          val newtypeargs = typeargs map { _ map { recurse(_) } }
          Call(newtarget, newargs, newtypeargs)
        }
        case Parallel(left, right) => recurse(left) || recurse(right)
        case Sequence(left, x, right) => recurse(left) > x > transform(right, x :: context, typecontext)
        case Graft(x, value, body) => Graft(x, recurse(value), transform(body, x :: context, typecontext))
        case Trim(f) => Trim(recurse(f))
        case Otherwise(left, right) => recurse(left) ow recurse(right)
        case New(self, st, bindings, t) => New(self, st.map(transform(_, typecontext)), Map() ++ bindings.mapValues(transform(_, self :: context, typecontext)), t.map(transform(_, typecontext)))
        case FieldAccess(o, f) => FieldAccess(recurse(o), f)
        case DeclareCallables(defs, body) => {
          val defnames = defs map { _.name }
          val newdefs = defs map { transform(_, defnames ::: context, typecontext) }
          val newbody = transform(body, defnames ::: context, typecontext)
          DeclareCallables(newdefs, newbody)
        }
        case DeclareType(u, t, body) => {
          val newt = transform(t, u :: typecontext)
          val newbody = transform(body, context, u :: typecontext)
          DeclareType(u, newt, newbody)
        }
        case HasType(body, expectedType) => HasType(recurse(body), recurse(expectedType))
        case Hole(context, typecontext) => Hole(context, typecontext)
        case VtimeZone(timeOrder, body) => VtimeZone(recurse(timeOrder), recurse(body))
      }
    }
  }

  def transform(t: Type, typecontext: List[BoundTypevar]): Type = {
    val pf = onType(typecontext)
    if (pf isDefinedAt t) {
      t -> pf
    } else {
      def recurse(t: Type) = transform(t, typecontext)
      t -> {
        case Bot() => Bot()
        case Top() => Top()
        case ImportedType(cl) => ImportedType(cl)
        case ClassType(cl) => ClassType(cl)
        case u: Typevar => u
        case TupleType(elements) => TupleType(elements map recurse)
        case RecordType(entries) => {
          val newEntries = entries map { case (s, t) => (s, recurse(t)) }
          RecordType(newEntries)
        }
        case TypeApplication(tycon, typeactuals) => {
          TypeApplication(recurse(tycon), typeactuals map recurse)
        }
        case AssertedType(assertedType) => AssertedType(recurse(assertedType))
        case FunctionType(typeformals, argtypes, returntype) => {
          val newtypecontext = typeformals ::: typecontext
          val newargtypes = argtypes map { transform(_, newtypecontext) }
          val newreturntype = transform(returntype, newtypecontext)
          FunctionType(typeformals, newargtypes, newreturntype)
        }
        case TypeAbstraction(typeformals, t) => {
          TypeAbstraction(typeformals, transform(t, typeformals ::: typecontext))
        }
        case VariantType(self, typeformals, variants) => {
          val newTypeContext = self :: typeformals ::: typecontext
          val newVariants =
            for ((name, variant) <- variants) yield {
              (name, variant map { transform(_, newTypeContext) })
            }
          VariantType(self, typeformals, newVariants)
        }
        case IntersectionType(a, b) => IntersectionType(recurse(a), recurse(b))
        case UnionType(a, b) => UnionType(recurse(a), recurse(b))
        case StructuralType(members) => StructuralType(members.mapValues(recurse))
        case NominalType(a) => NominalType(recurse(a))
      }
    }
  }

  def transform(d: Callable, context: List[BoundVar], typecontext: List[BoundTypevar]): Callable = {
    val pf = onCallable(context, typecontext)
    if (pf isDefinedAt d) {
      d -> pf
    } else {
      d -> {
        case Callable(name, formals, body, typeformals, argtypes, returntype) => {
          val newcontext = formals ::: context
          val newtypecontext = typeformals ::: typecontext
          val newbody = transform(body, newcontext, newtypecontext)
          val newargtypes = argtypes map { _ map { transform(_, newtypecontext) } }
          val newreturntype = returntype map { transform(_, newtypecontext) }
          d.copy(name, formals, newbody, typeformals, newargtypes, newreturntype)
        }
      }
    }
  }
}
