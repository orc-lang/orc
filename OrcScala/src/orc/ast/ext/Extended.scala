//
// Extended.scala -- Scala class and objects for the Orc extended AST
// Project OrcScala
//
// Created by dkitchin on May 19, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
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
sealed case class Pruning(left: Expression, p: Option[Pattern] = None, right: Expression) extends Expression
sealed case class Otherwise(left: Expression, right: Expression) extends Expression
sealed case class Lambda(
  typeformals: Option[List[String]] = None,
  formals: List[Pattern],
  returntype: Option[Type] = None,
  guard: Option[Expression] = None,
  body: Expression) extends Expression

sealed case class Conditional(ifE: Expression, thenE: Expression, elseE: Expression) extends Expression
sealed case class Declare(declaration: Declaration, body: Expression) extends Expression
sealed case class TypeAscription(e: Expression, t: Type) extends Expression
sealed case class TypeAssertion(e: Expression, t: Type) extends Expression

// An internal representation for the body of a 'def class'
sealed case class DefClassBody(body: Expression) extends Expression

sealed abstract class Declaration extends AST

sealed case class Val(p: Pattern, e: Expression) extends Declaration
sealed case class Include(origin: String, decls: List[Declaration]) extends Declaration

sealed abstract class NamedDeclaration extends Declaration {
  val name: String
}

sealed abstract class DefDeclaration extends NamedDeclaration
sealed case class Def(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type], guard: Option[Expression], body: Expression) extends DefDeclaration
sealed case class DefClass(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type], guard: Option[Expression], body: Expression) extends DefDeclaration
sealed case class DefSig(name: String, typeformals: Option[List[String]], argtypes: List[Type], returntype: Type) extends DefDeclaration

// Convenience extractor for sequences of definitions enclosing some scope
object DefGroup {
  def unapply(e: Expression): Option[(List[DefDeclaration], Expression)] = {
    partition(e) match {
      case (Nil, _) => None
      case (ds, f) => Some((ds, f))
    }
  }

  private def partition(e: Expression): (List[DefDeclaration], Expression) = {
    e match {
      case Declare(d: DefDeclaration, f) => {
        val (ds, g) = partition(f)
        (d :: ds, g)
      }
      case _ => (Nil, e)
    }
  }

}

sealed abstract class SiteDeclaration extends NamedDeclaration
sealed case class SiteImport(name: String, sitename: String) extends SiteDeclaration
sealed case class ClassImport(name: String, classname: String) extends SiteDeclaration

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
