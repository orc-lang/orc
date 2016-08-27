//
// Transformation.scala -- Scala traits NamedASTFunction and NamedASTTransform and object EmptyFunction
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 12, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.orctimizer.named

import scala.language.reflectiveCalls

/** @author dkitchin
  */
trait NamedASTFunction {
  def apply(a: Argument): Argument
  def apply(e: Expression): Expression
  def apply(t: Type): Type
  def apply(d: Def): Def

  def apply(ast: NamedAST): NamedAST = {
    ast match {
      case a: Argument => this(a)
      case e: Expression => this(e)
      case t: Type => this(t)
      case d: Def => this(d)
    }
  }

  def andThen(g: NamedASTFunction): NamedASTFunction = {
    val f = this
    new NamedASTFunction {
      def apply(a: Argument): Argument = g(f(a))
      def apply(e: Expression): Expression = g(f(e))
      def apply(t: Type): Type = g(f(t))
      def apply(d: Def): Def = g(f(d))
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
  def apply(d: Def): Def = transform(d, Nil, Nil)

  def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]): PartialFunction[Expression, Expression] = EmptyFunction

  def onArgument(context: List[BoundVar]): PartialFunction[Argument, Argument] = EmptyFunction

  def onType(typecontext: List[BoundTypevar]): PartialFunction[Type, Type] = EmptyFunction

  def onDef(context: List[BoundVar], typecontext: List[BoundTypevar]): PartialFunction[Def, Def] = EmptyFunction

  def recurseWithContext(context: List[BoundVar], typecontext: List[BoundTypevar]) =
    new NamedASTFunction {
      def apply(a: Argument) = transform(a, context)
      def apply(e: Expression) = transform(e, context, typecontext)
      def apply(t: Type) = transform(t, typecontext)
      def apply(d: Def) = transform(d, context, typecontext)
    }

  def transform(a: Argument, context: List[BoundVar]): Argument = {
    val pf = onArgument(context)
    if (pf isDefinedAt a) { a -> pf } else a
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
        case CallDef(target, args, typeargs) => {
          val newtarget = recurse(target)
          val newargs = args map { recurse(_) }
          val newtypeargs = typeargs map { _ map { recurse(_) } }
          CallDef(newtarget, newargs, newtypeargs)
        }
        case CallSite(target, args, typeargs) => {
          val newtarget = recurse(target)
          val newargs = args map { recurse(_) }
          val newtypeargs = typeargs map { _ map { recurse(_) } }
          CallSite(newtarget, newargs, newtypeargs)
        }
        case left || right => recurse(left) || recurse(right)
        case left > x > right => recurse(left) > x > transform(right, x :: context, typecontext)
        case Trim(f) => Trim(recurse(f))
        case Force(xs, vs, b, e) => {
          val newvs = vs map { recurse(_) }
          Force(xs, newvs, b, transform(e, xs ::: context, typecontext))
        }
        case Future(x, f, g) => Future(x, recurse(f), transform(g, x :: context, typecontext))
        case left Otherwise right => Otherwise(recurse(left), recurse(right))
        case DeclareDefs(defs, body) => {
          val defnames = defs map { _.name }
          val newdefs = defs map { transform(_, defnames ::: context, typecontext) }
          val newbody = transform(body, defnames ::: context, typecontext)
          DeclareDefs(newdefs, newbody)
        }
        case DeclareType(u, t, body) => {
          val newt = transform(t, u :: typecontext)
          val newbody = transform(body, context, u :: typecontext)
          DeclareType(u, newt, newbody)
        }
        case HasType(body, expectedType) => HasType(recurse(body), recurse(expectedType))
        case VtimeZone(timeOrder, body) => VtimeZone(recurse(timeOrder), recurse(body))
        case FieldAccess(o, f) => FieldAccess(recurse(o), f)
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
      }
    }
  }

  def transform(d: Def, context: List[BoundVar], typecontext: List[BoundTypevar]): Def = {
    val pf = onDef(context, typecontext)
    if (pf isDefinedAt d) {
      d -> pf
    } else {
      d -> {
        case Def(name, formals, body, typeformals, argtypes, returntype) => {
          val newcontext = formals ::: context
          val newtypecontext = typeformals ::: typecontext
          val newbody = transform(body, newcontext, newtypecontext)
          val newargtypes = argtypes map { _ map { transform(_, newtypecontext) } }
          val newreturntype = returntype map { transform(_, newtypecontext) }
          Def(name, formals, newbody, typeformals, newargtypes, newreturntype)
        }
      }
    }
  }

}
