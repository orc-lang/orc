//
// ContextualTransform.scala -- Scala class/trait/object ContextualTransform
// Project OrcScala
//
// Created by amp on May 31, 2013.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.orctimizer.named

import Bindings._

trait ContextualTransform extends NamedASTFunction {
  import Bindings._

  def order[E <: NamedAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E

  def apply(a: Argument): Argument = transform(a)(TransformContext())
  def apply(e: Expression): Expression = transform(e)(TransformContext())
  def apply(t: Type): Type = transform(t)(TransformContext())
  def apply(d: Callable): Callable = transform(d)(TransformContext())
  def apply(d: FieldValue): FieldValue = transform(d)(TransformContext())

  def onExpression(implicit ctx: TransformContext): PartialFunction[Expression, Expression] = new PartialFunction[Expression, Expression] {
    def isDefinedAt(e: Expression) = pf.isDefinedAt(e in ctx)
    def apply(e: Expression) = pf(e in ctx)

    val pf = onExpressionCtx
  }
  def onExpressionCtx: PartialFunction[WithContext[Expression], Expression] = EmptyFunction

  def onArgument(implicit ctx: TransformContext): PartialFunction[Argument, Argument] = EmptyFunction

  def onType(implicit ctx: TransformContext): PartialFunction[Type, Type] = EmptyFunction

  def onCallable(implicit ctx: TransformContext): PartialFunction[Callable, Callable] = EmptyFunction

  def onFieldValue(implicit ctx: TransformContext): PartialFunction[FieldValue, FieldValue] = EmptyFunction

  def recurseWithContext(implicit ctx: TransformContext) =
    new NamedASTFunction {
      def apply(a: Argument) = transform(a)
      def apply(e: Expression) = transform(e)
      def apply(t: Type) = transform(t)
      def apply(d: Callable) = transform(d)
      def apply(d: FieldValue) = transform(d)
    }

  def transform(a: Argument)(implicit ctx: TransformContext): Argument = {
    order[Argument](onArgument, (x: Argument) => x)(a)
    // FIXME: This is probably going to cause variable renaming.
  }

  def transform(expr: Expression)(implicit ctx: TransformContext): Expression = {
    val recurse = recurseWithContext
    order[Expression](onExpression, {
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
      case left Parallel right => recurse(left) || recurse(right)
      case e @ Branch(left, x, right) => recurse(left) > x > transform(right)(ctx + SeqBound(ctx, e.asInstanceOf[Branch]))
      case Trim(f) => Trim(recurse(f))
      case expr @ Force(xs, vs, b, e) => {
        val newvs = vs map { recurse(_) }
        Force(xs, newvs, b, transform(e)(ctx extendBindings (xs.map(x => ForceBound(ctx, expr, x)))))
      }
      case e @ Future(f) => Future(recurse(f))
      case left Otherwise right => Otherwise(recurse(left), recurse(right))
      case IfDef(a, f, g) => IfDef(recurse(a), recurse(f), recurse(g))
      case e @ DeclareCallables(defs, body) => {
        val newctxrec = ctx extendBindings (defs map { RecursiveCallableBound(ctx, e, _) })
        val newdefs = defs map { transform(_)(newctxrec) }
        val newctx = ctx extendBindings (defs map { CallableBound(newctxrec, e, _) })
        val newbody = transform(body)(newctx)
        DeclareCallables(newdefs, newbody)
      }
      case e @ DeclareType(u, t, body) => {
        val newctx = ctx + TypeBinding(ctx, u)
        val newt = transform(t)(newctx)
        val newbody = transform(body)(newctx)
        DeclareType(u, newt, newbody)
      }
      case HasType(body, expectedType) => HasType(recurse(body), recurse(expectedType))
      //case VtimeZone(timeOrder, body) => VtimeZone(recurse(timeOrder), recurse(body))
      case FieldAccess(o, f) => FieldAccess(recurse(o), f)
    })(expr)
  }

  def transform(t: Type)(implicit ctx: TransformContext): Type = {
    def recurse(t: Type) = transform(t)
    order[Type](onType, {
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
        val newtypecontext = ctx.extendTypeBindings(typeformals map { TypeBinding(ctx, _) })
        val newargtypes = argtypes map { transform(_)(newtypecontext) }
        val newreturntype = transform(returntype)(newtypecontext)
        FunctionType(typeformals, newargtypes, newreturntype)
      }
      case TypeAbstraction(typeformals, t) => {
        TypeAbstraction(typeformals, transform(t)(ctx.extendTypeBindings(typeformals map { TypeBinding(ctx, _) })))
      }
      case VariantType(self, typeformals, variants) => {
        val newTypeContext = ctx.extendTypeBindings(typeformals map { TypeBinding(ctx, _) }) + TypeBinding(ctx, self)
        val newVariants =
          for ((name, variant) <- variants) yield {
            (name, variant map { transform(_)(newTypeContext) })
          }
        VariantType(self, typeformals, newVariants)
      }
    })(t)
  }

  def transform(d: Callable)(implicit ctx: TransformContext): Callable = {
    order[Callable](onCallable, {
      case d @ Def(name, formals, body, typeformals, argtypes, returntype) => {
        val newcontext = ctx extendBindings (formals map { ArgumentBound(ctx, d, _) }) extendTypeBindings (typeformals map { TypeBinding(ctx, _) })
        val newbody = transform(body)(newcontext)
        val newargtypes = argtypes map { _ map { transform(_)(newcontext) } }
        val newreturntype = returntype map { transform(_)(newcontext) }
        Def(name, formals, newbody, typeformals, newargtypes, newreturntype)
      }
      case d @ Site(name, formals, body, typeformals, argtypes, returntype) => {
        val newcontext = ctx extendBindings (formals map { ArgumentBound(ctx, d, _) }) extendTypeBindings (typeformals map { TypeBinding(ctx, _) })
        val newbody = transform(body)(newcontext)
        val newargtypes = argtypes map { _ map { transform(_)(newcontext) } }
        val newreturntype = returntype map { transform(_)(newcontext) }
        Def(name, formals, newbody, typeformals, newargtypes, newreturntype)
      }
    })(d)
  }

  def transform(d: FieldValue)(implicit ctx: TransformContext): FieldValue = {
    order[FieldValue](onFieldValue, {
      case FieldFuture(e) => FieldFuture(transform(e))
      case FieldArgument(e) => FieldArgument(transform(e))
    })(d)
  }

}

object ContextualTransform {
  trait NonDescending extends ContextualTransform {
    def order[E <: NamedAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      pf.applyOrElse(e, descend)
    }
  }
  trait Pre extends ContextualTransform {
    def order[E <: NamedAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      val e1 = pf.lift(e).getOrElse(e)
      e1 ->> descend(e1)
    }
  }
  trait Post extends ContextualTransform {
    def order[E <: NamedAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      val e1 = e ->> descend(e)
      pf.lift(e1).getOrElse(e1)
    }
  }
}
