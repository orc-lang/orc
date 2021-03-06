//
// Extended.scala -- Scala class and objects for the Orc extended AST
// Project OrcScala
//
// Created by dkitchin on May 19, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.ext

import orc.ast.{ AST, OrcSyntaxConvertible }

sealed abstract class Expression extends AST

sealed case class Stop() extends Expression
sealed case class Constant(c: AnyRef) extends Expression
sealed case class Variable(name: String) extends Expression
sealed case class TupleExpr(elements: List[Expression]) extends Expression { require(elements.size > 1) }
sealed case class ListExpr(elements: List[Expression]) extends Expression
sealed case class RecordExpr(elements: List[(String, Expression)]) extends Expression
sealed case class Call(target: Expression, gs: List[ArgumentGroup]) extends Expression
case object Hole extends Expression

sealed abstract class ArgumentGroup extends AST
sealed case class Args(types: Option[List[Type]] = None, elements: List[Expression]) extends ArgumentGroup
sealed case class FieldAccess(field: String) extends ArgumentGroup
case object Dereference extends ArgumentGroup

sealed case class PrefixOperator(op: String, arg: Expression) extends Expression
sealed case class InfixOperator(left: Expression, op: String, right: Expression) extends Expression
sealed case class Sequential(left: Expression, p: Option[Pattern] = None, right: Expression) extends Expression
sealed case class Parallel(left: Expression, right: Expression) extends Expression
sealed case class Trim(expr: Expression) extends Expression
sealed case class Otherwise(left: Expression, right: Expression) extends Expression
sealed case class Lambda(
  typeformals: Option[List[String]] = None,
  formals: List[Pattern],
  returntype: Option[Type] = None,
  //guard: Option[Expression] = None, TODO: guards on lambdas are not supported any more
  body: Expression) extends Expression

// TODO: Rename "Section" to something better.
sealed case class Section(body: Expression) extends Expression
sealed case class Placeholder() extends Expression

sealed case class Conditional(ifE: Expression, thenE: Expression, elseE: Expression) extends Expression
sealed case class Declare(declaration: Declaration, body: Expression) extends Expression
sealed case class TypeAscription(e: Expression, t: Type) extends Expression
sealed case class TypeAssertion(e: Expression, t: Type) extends Expression

sealed abstract class Declaration extends AST

sealed case class Val(p: Pattern, e: Expression) extends Declaration

/** Unlike vals val signatures can only assign a single type to a name. They only appear in classes.
  */
sealed case class ValSig(name: String, t: Option[Type]) extends Declaration

sealed case class Include(origin: String, decls: List[Declaration]) extends Declaration

sealed abstract class NamedDeclaration extends Declaration {
  val name: String
}

sealed abstract class CallableDeclaration extends NamedDeclaration {
  def sameKindAs(decl: CallableDeclaration): Boolean
  def arity: Int
}

sealed trait DefDeclaration extends CallableDeclaration {
  def sameKindAs(decl: CallableDeclaration): Boolean = decl.isInstanceOf[DefDeclaration]
}
sealed trait SiteDeclaration extends CallableDeclaration {
  def sameKindAs(decl: CallableDeclaration): Boolean = decl.isInstanceOf[SiteDeclaration]
}

sealed abstract class Callable extends CallableDeclaration {
  val name: String
  val typeformals: Option[List[String]]
  val formals: List[Pattern]
  val returntype: Option[Type]
  val guard: Option[Expression]
  val body: Expression

  def copy(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type], guard: Option[Expression], body: Expression): Callable

  def arity = formals.size
}
object Callable {
  def unapply(value: Callable) = {
    Some((value.name, value.typeformals, value.formals, value.returntype, value.guard, value.body))
  }
}

sealed abstract class CallableSig extends CallableDeclaration {
  val name: String
  val typeformals: Option[List[String]]
  val argtypes: List[Type]
  val returntype: Type

  def copy(name: String, typeformals: Option[List[String]], argtypes: List[Type], returntype: Type): CallableSig

  def arity = argtypes.size
}
object CallableSig {
  def unapply(value: CallableSig) = {
    Some((value.name, value.typeformals, value.argtypes, value.returntype))
  }
}

sealed case class Def(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type], guard: Option[Expression], body: Expression) extends Callable with DefDeclaration {
  def copy(name: String = name, typeformals: Option[List[String]] = typeformals, formals: List[Pattern] = formals, returntype: Option[Type] = returntype, guard: Option[Expression] = guard, body: Expression = body): Def = {
    Def(name, typeformals, formals, returntype, guard, body)
  }
}
sealed case class DefSig(name: String, typeformals: Option[List[String]], argtypes: List[Type], returntype: Type) extends CallableSig with DefDeclaration {
  def copy(name: String = name, typeformals: Option[List[String]] = typeformals, argtypes: List[Type] = argtypes, returntype: Type = returntype): DefSig = {
    DefSig(name, typeformals, argtypes, returntype)
  }
}

sealed case class Site(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type], guard: Option[Expression], body: Expression) extends Callable with SiteDeclaration {
  def copy(name: String = name, typeformals: Option[List[String]] = typeformals, formals: List[Pattern] = formals, returntype: Option[Type] = returntype, guard: Option[Expression] = guard, body: Expression = body): Site = {
    Site(name, typeformals, formals, returntype, guard, body)
  }
}
sealed case class SiteSig(name: String, typeformals: Option[List[String]], argtypes: List[Type], returntype: Type) extends CallableSig with SiteDeclaration {
  def copy(name: String = name, typeformals: Option[List[String]] = typeformals, argtypes: List[Type] = argtypes, returntype: Type = returntype): SiteSig = {
    SiteSig(name, typeformals, argtypes, returntype)
  }
}

// TODO: Factor out the shared code between CallableGroup and CallableSingle
/** Convenience extractor for sequences of definitions enclosing some scope
  * The extractor will extract sites OR defs, but never both.
  */
object CallableGroup {
  def unapply(e: Expression): Option[(List[CallableDeclaration], Expression)] = {
    partition(e, None) match {
      case (Nil, _) => None
      case (ds, f) => Some((ds, f))
    }
  }

  private def partition(e: Expression, kindSample: Option[CallableDeclaration]): (List[CallableDeclaration], Expression) = {
    e match {
      case Declare(d: CallableDeclaration, f) if kindSample.isEmpty || (kindSample.get sameKindAs d) => {
        val (ds, g) = partition(f, Some(d))
        (d :: ds, g)
      }
      case _ => (Nil, e)
    }
  }

}

object CallableSingle {
  def unapply(ds: Seq[Declaration]): Option[(List[CallableDeclaration], Seq[Declaration])] = {
    partition(ds, None) match {
      case (Nil, _) => None
      case (ds, f) => Some((ds, f))
    }
  }

  private def partition(e: Seq[Declaration], kindSample: Option[CallableDeclaration]): (List[CallableDeclaration], Seq[Declaration]) = {
    e match {
      case (d: CallableDeclaration) :: rest if kindSample.isEmpty || ((kindSample.get sameKindAs d) && kindSample.get.name == d.name) => {
        val (ds, g) = partition(rest, Some(d))
        (d :: ds, g)
      }
      case _ => (Nil, e)
    }
  }
}

sealed case class SiteImport(name: String, sitename: String) extends NamedDeclaration
sealed case class ClassImport(name: String, classname: String) extends NamedDeclaration

sealed case class ClassDeclaration(constructor: ClassConstructor, superclass: Option[ClassExpression], body: ClassLiteral) extends NamedDeclaration {
  val name = constructor.name

  def classExpression = superclass match {
    case Some(sc) => this ->> ClassSubclassLiteral(sc, body)
    case None => body
  }
}

sealed abstract class ClassExpression extends AST {
  def toInterfaceString: String
  def containsLiteral: Boolean
}
case class ClassVariable(name: String) extends ClassExpression {
  def toInterfaceString = name
  def containsLiteral = false
}
case class ClassLiteral(thisname: Option[String], decls: List[Declaration]) extends ClassExpression {
  def toInterfaceString = "{ ... }"
  def toDetailedInterfaceString = decls.collect({
    case v: Val => v.p.toOrcSyntax
    case d: NamedDeclaration => d.name
  }).mkString("{ ", ", ", " }")
  def containsLiteral = true
}
case class ClassSubclassLiteral(superclass: ClassExpression, body: ClassLiteral) extends ClassExpression {
  def toInterfaceString = s"(${superclass.toInterfaceString}) ${body.toInterfaceString}"
  def containsLiteral = true
}
case class ClassMixin(left: ClassExpression, right: ClassExpression) extends ClassExpression {
  def toInterfaceString = left.toInterfaceString + " with " + right.toInterfaceString
  def containsLiteral = left.containsLiteral || right.containsLiteral
}

/** Match mixed classes.
  *
  * The resulting sequence is in linearization order. (A with B with C) return List(C, B, A).
  */
object ClassMixins {
  def unapplySeq(e: ClassExpression): Option[List[ClassExpression]] = e match {
    case ClassMixin(l, r) => {
      Some(elements(r) ++ elements(l))
    }
    case e => None
  }

  private def elements(e: ClassExpression): List[ClassExpression] = e match {
    case ClassMixin(l, r) => {
      elements(r) ++ elements(l)
    }
    case e => List(e)
  }
}

sealed abstract class ClassConstructor extends AST {
  val name: String
  val typeformals: Option[List[String]]
}
sealed abstract class ClassCallableConstructor extends ClassConstructor {
  val formals: List[Pattern]
  val returntype: Option[Type]

  def copy(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type]): ClassCallableConstructor
}
object ClassConstructor {
  case class None(name: String, typeformals: Option[List[String]]) extends ClassConstructor
  case class Def(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type]) extends ClassCallableConstructor {
    def copy(name: String = name, typeformals: Option[List[String]] = typeformals, formals: List[Pattern] = formals, returntype: Option[Type] = returntype): Def = {
      Def(name, typeformals, formals, returntype)
    }
  }
  case class Site(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type]) extends ClassCallableConstructor {
    def copy(name: String = name, typeformals: Option[List[String]] = typeformals, formals: List[Pattern] = formals, returntype: Option[Type] = returntype): Site = {
      Site(name, typeformals, formals, returntype)
    }
  }
}

sealed case class New(cls: ClassExpression) extends Expression

object ClassDefGroup {
  def unapply(e: Expression): Option[(List[ClassDeclaration], List[DefDeclaration], Expression)] = {
    partition(e) match {
      case (Nil, Nil, _) => None
      case (cs, ds, f) => Some((cs, ds, f))
    }
  }

  private def partition(e: Expression): (List[ClassDeclaration], List[DefDeclaration], Expression) = {
    e match {
      case Declare(d: ClassDeclaration, f) => {
        val (cs, ds,  g) = partition(f)
        (d :: cs, ds, g)
      }
      case Declare(d: DefDeclaration, f)  => {
        val (cs, ds, g) = partition(f)
        (cs, d :: ds, g)
      }
      case _ => (Nil, Nil, e)
    }
  }

}

sealed abstract class TypeDeclaration extends NamedDeclaration
sealed case class TypeAlias(name: String, typeformals: List[String] = Nil, aliasedtype: Type) extends TypeDeclaration
sealed case class TypeImport(name: String, classname: String) extends TypeDeclaration
sealed case class Datatype(name: String, typeformals: List[String] = Nil, constructors: List[Constructor]) extends TypeDeclaration

sealed case class Constructor(name: String, types: List[Option[Type]]) extends AST

sealed abstract class Pattern extends AST with OrcSyntaxConvertible {
  val isStrict: Boolean
}

sealed abstract class NonStrictPattern extends Pattern {
  val isStrict = false
}
sealed case class Wildcard() extends NonStrictPattern { override def toOrcSyntax = "_" }
sealed case class VariablePattern(name: String) extends NonStrictPattern { override def toOrcSyntax = name }

sealed abstract class StrictPattern extends Pattern {
  val isStrict = true
}
sealed case class ConstantPattern(c: AnyRef) extends StrictPattern { override def toOrcSyntax = if (c == null) "null" else c.toString }
sealed case class TuplePattern(elements: List[Pattern]) extends StrictPattern { override def toOrcSyntax = elements.map(_.toOrcSyntax).mkString("(", ", ", ")") }
sealed case class ListPattern(elements: List[Pattern]) extends StrictPattern { override def toOrcSyntax = elements.map(_.toOrcSyntax).mkString("[", ", ", "]") }
sealed case class CallPattern(name: String, args: List[Pattern]) extends StrictPattern { override def toOrcSyntax = name + args.map(_.toOrcSyntax).mkString("(", ", ", ")") }
sealed case class ConsPattern(head: Pattern, tail: Pattern) extends StrictPattern { override def toOrcSyntax = "(" + head.toOrcSyntax + ":" + tail.toOrcSyntax + ")" }
sealed case class RecordPattern(elements: List[(String, Pattern)]) extends StrictPattern { override def toOrcSyntax = elements.map({ case (f, p) => f + " = " + p.toOrcSyntax }).mkString("{. ", ", ", " .}") }

sealed case class AsPattern(p: Pattern, name: String) extends Pattern {
  val isStrict = p.isStrict
  override def toOrcSyntax = p.toOrcSyntax + " as " + name
}
sealed case class TypedPattern(p: Pattern, t: Type) extends Pattern {
  val isStrict = p.isStrict
  override def toOrcSyntax = p.toOrcSyntax + " :: " + t.toOrcSyntax
}

sealed abstract class Type extends AST with OrcSyntaxConvertible

sealed case class TypeVariable(name: String) extends Type { override def toOrcSyntax = name }
sealed case class TupleType(elements: List[Type]) extends Type { override def toOrcSyntax = elements.map(_.toOrcSyntax).mkString("(", ", ", ")") }
sealed case class RecordType(elements: List[(String, Type)]) extends Type { override def toOrcSyntax = elements.map({ case (f, t) => f + " :: " + t.toOrcSyntax }).mkString("{. ", ", ", " .}") }
sealed case class LambdaType(typeformals: List[String], argtypes: List[Type], returntype: Type) extends Type {
  override def toOrcSyntax = "lambda" + (if (typeformals.size > 0) typeformals.mkString("[", ", ", "]") else "") + argtypes.map(_.toOrcSyntax).mkString("(", ", ", ")") + " :: " + returntype.toOrcSyntax

}
sealed case class TypeApplication(name: String, typeactuals: List[Type]) extends Type { override def toOrcSyntax = name + typeactuals.map(_.toOrcSyntax).mkString("[", ", ", "]") }
