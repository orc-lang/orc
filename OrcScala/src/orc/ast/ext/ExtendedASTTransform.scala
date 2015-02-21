//
// Transformation.scala -- Scala traits ExtendedASTFunction and ExtendedASTTransform and object EmptyFunction
// Project OrcScala
//
// $Id: NamedASTTransform.scala 3368 2014-12-06 01:55:28Z arthur.peters@gmail.com $
//
// Created by dkitchin on Jul 12, 2010.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.ext

import scala.language.reflectiveCalls

/** @author dkitchin
  */
trait ExtendedASTFunction {
  def apply(e: Expression): Expression
  def apply(t: Type): Type
  def apply(a: ArgumentGroup): ArgumentGroup
  def apply(d: Declaration): Declaration
  def apply(a: Pattern): Pattern
  def apply(a: ClassExpression): ClassExpression

  def andThen(g: ExtendedASTFunction): ExtendedASTFunction = {
    val f = this
    new ExtendedASTFunction {
      def apply(e: Expression): Expression = g(f(e))
      def apply(t: Type): Type = g(f(t))
      def apply(a: ArgumentGroup): ArgumentGroup = g(f(a))
      def apply(d: Declaration): Declaration = g(f(d))
      def apply(a: Pattern): Pattern = g(f(a))
      def apply(a: ClassExpression): ClassExpression = g(f(a))
    }
  }

}

object EmptyFunction extends PartialFunction[Any, Nothing] {
  def isDefinedAt(x: Any): Boolean = false
  def apply(x: Any): Nothing = throw new AssertionError("EmptyFunction is undefined for all inputs.")
}

trait ExtendedASTTransform extends ExtendedASTFunction {
  def apply(e: Expression): Expression = transform(e)
  def apply(t: Type): Type = transform(t)
  def apply(a: ArgumentGroup): ArgumentGroup = transform(a)
  def apply(d: Declaration): Declaration = transform(d)
  def apply(a: Pattern): Pattern = transform(a)
  def apply(a: ClassExpression): ClassExpression = transform(a)

  def onExpression(): PartialFunction[Expression, Expression] = EmptyFunction
  def onType(): PartialFunction[Type, Type] = EmptyFunction
  def onArgumentGroup(): PartialFunction[ArgumentGroup, ArgumentGroup] = EmptyFunction
  def onDeclaration(): PartialFunction[Declaration, Declaration] = EmptyFunction
  def onPattern(): PartialFunction[Pattern, Pattern] = EmptyFunction
  def onClassExpression(): PartialFunction[ClassExpression, ClassExpression] = EmptyFunction

  def transform(e: Expression): Expression = {
    val pf = onExpression()
    if (pf isDefinedAt e) { e -> pf } else {
      val recurse: Expression => Expression = this.apply
      e -> {
        case Stop() | Constant(_) | Variable(_) | Hole | Placeholder() => e
        case TupleExpr(es) => TupleExpr(es map recurse)
        case ListExpr(es) => ListExpr(es map recurse)
        case RecordExpr(es) => RecordExpr(es map { p => (p._1, recurse(p._2)) })
        case Call(t, gs) => Call(recurse(t), gs map this.apply)
        case PrefixOperator(op, e) => PrefixOperator(op, recurse(e))
        case InfixOperator(f, op, g) => InfixOperator(recurse(f), op, recurse(g))
        case Sequential(f, p, g) => Sequential(recurse(f), p map this.apply, recurse(g))
        case Parallel(f, g) => Parallel(recurse(f), recurse(g))
        case Otherwise(f, g) => Otherwise(recurse(f), recurse(g))
        case Trim(f) => Trim(recurse(f))
        case New(s) => New(this(s))
        case Section(f) => Section(recurse(f))
        case Conditional(f, g, e) => Conditional(recurse(f), recurse(g), recurse(e))
        case Declare(d, body) => Declare(this(d), recurse(body))
        case TypeAscription(e, t) => TypeAscription(recurse(e), this(t))
        case TypeAssertion(e, t) => TypeAssertion(recurse(e), this(t))
      }
    }
  }

  def transform(d: Declaration): Declaration = {
    val pf = onDeclaration()
    if (pf isDefinedAt d) { d -> pf } else {
      d -> {
        case SiteImport(_, _) | ClassImport(_, _) | TypeImport(_, _) => d
        case Val(p, e) => Val(this(p), this(e))
        case ValSig(p, t) => ValSig(p, this(t))
        case Include(o, decls) => Include(o, decls map this.apply)
        case ClassDeclaration(name, base, body) =>
          ClassDeclaration(name, base map this.apply, ClassLiteral(body.thisname, body.decls map this.apply))
        case c @ Callable(name, typeformals, formals, returntype, guard, body) =>
          c.copy(name, typeformals, formals map this.apply, returntype map this.apply, guard map this.apply, this(body))
        case c @ CallableSig(name, typeformals, argtypes, returntype) =>
          c.copy(name, typeformals, argtypes map this.apply, this(returntype))
        case TypeAlias(name, formals, aliased) => TypeAlias(name, formals, this(aliased))
        //case Datatype(name, formals, constructors) => TypeAlias(name, formals, constructors map this.apply)
        case Datatype(name, formals, constructors) =>
          Datatype(name, formals, constructors map { c => Constructor(c.name, c.types map { _ map this.apply }) })
      }
    }
  }

  def transform(t: ClassExpression): ClassExpression = {
    val pf = onClassExpression()
    if (pf isDefinedAt t) { t -> pf } else {
      t -> {
        case ClassVariable(_) => t
        case ClassLiteral(self, ds) => ClassLiteral(self, ds map this.apply)
        case ClassMixin(l, r) => ClassMixin(this(l), this(r))
        case ClassSubclassLiteral(l, r) => ClassSubclassLiteral(this(l), this(r).asInstanceOf[ClassLiteral])
      }
    }
  }

  def transform(p: Pattern): Pattern = {
    val pf = onPattern()
    if (pf isDefinedAt p) { p -> pf } else {
      p -> {
        case Wildcard() | VariablePattern(_) | ConstantPattern(_) => p
        case TuplePattern(ps) => TuplePattern(ps map this.apply)
        case ListPattern(ps) => ListPattern(ps map this.apply)
        case RecordPattern(es) => RecordPattern(es map { p => (p._1, this(p._2)) })
        case CallPattern(name, args) => CallPattern(name, args map this.apply)
        case ConsPattern(f, g) => ConsPattern(this(f), this(g))
        case AsPattern(p, name) => AsPattern(this(p), name)
        case TypedPattern(p, t) => TypedPattern(this(p), this(t))
      }
    }
  }

  def transform(t: Type): Type = {
    val pf = onType()
    if (pf isDefinedAt t) { t -> pf } else {
      t -> {
        case TypeVariable(_) => t
        case TupleType(ts) => TupleType(ts map this.apply)
        case RecordType(es) => RecordType(es map { p => (p._1, this(p._2)) })
        case LambdaType(typeformals, argtypes, returntype) => LambdaType(typeformals, argtypes map this.apply, this(returntype))
        case TypeApplication(name, actuals) => TypeApplication(name, actuals map this.apply)
      }
    }
  }

  def transform(a: ArgumentGroup): ArgumentGroup = {
    val pf = onArgumentGroup()
    if (pf isDefinedAt a) { a -> pf } else {
      a -> {
        case Dereference | FieldAccess(_) => a
        case Args(types, elements) => Args(types map { _ map this.apply }, elements map this.apply)
      }
    }
  }
}
