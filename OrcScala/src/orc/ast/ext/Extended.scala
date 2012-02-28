//
// Extended.scala -- Scala class and objects for the Orc extended AST
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 19, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.ext

import orc.ast.AST
import orc.ast.OrcSyntaxConvertible

sealed abstract class Expression extends AST

case class Stop() extends Expression
case class Constant(c: AnyRef) extends Expression
case class Variable(name: String) extends Expression
case class TupleExpr(elements: List[Expression]) extends Expression { require(elements.size > 1) }
case class ListExpr(elements: List[Expression]) extends Expression
case class RecordExpr(elements: List[(String, Expression)]) extends Expression
case class Call(target: Expression, gs: List[ArgumentGroup]) extends Expression
case object Hole extends Expression

sealed abstract class ArgumentGroup extends AST
case class Args(types: Option[List[Type]] = None, elements: List[Expression]) extends ArgumentGroup
case class FieldAccess(field: String) extends ArgumentGroup
case object Dereference extends ArgumentGroup

case class PrefixOperator(op: String, arg: Expression) extends Expression
case class InfixOperator(left: Expression, op: String, right: Expression) extends Expression
case class Sequential(left: Expression, p: Option[Pattern] = None, right: Expression) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Pruning(left: Expression, p: Option[Pattern] = None, right: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression
case class Lambda(
  typeformals: Option[List[String]] = None,
  formals: List[Pattern],
  returntype: Option[Type] = None,
  guard: Option[Expression] = None,
  body: Expression) extends Expression

case class Conditional(ifE: Expression, thenE: Expression, elseE: Expression) extends Expression
case class Declare(declaration: Declaration, body: Expression) extends Expression
case class TypeAscription(e: Expression, t: Type) extends Expression
case class TypeAssertion(e: Expression, t: Type) extends Expression

//Expression (1+2)@A
case class SecurityLevelExpression(e: Expression, level: String) extends Expression

// An internal representation for the body of a 'def class'
case class DefClassBody(body: Expression) extends Expression

sealed abstract class Declaration extends AST

case class Val(p: Pattern, e: Expression) extends Declaration
case class Include(origin: String, decls: List[Declaration]) extends Declaration

sealed abstract class NamedDeclaration extends Declaration {
  val name: String
}

sealed abstract class DefDeclaration extends NamedDeclaration
case class Def(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type], guard: Option[Expression], body: Expression) extends DefDeclaration
case class DefClass(name: String, typeformals: Option[List[String]], formals: List[Pattern], returntype: Option[Type], guard: Option[Expression], body: Expression) extends DefDeclaration
case class DefSig(name: String, typeformals: Option[List[String]], argtypes: List[Type], returntype: Type) extends DefDeclaration

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
case class SiteImport(name: String, sitename: String) extends SiteDeclaration
case class ClassImport(name: String, classname: String) extends SiteDeclaration

sealed abstract class TypeDeclaration extends NamedDeclaration
case class TypeAlias(name: String, typeformals: List[String] = Nil, aliasedtype: Type) extends TypeDeclaration
case class TypeImport(name: String, classname: String) extends TypeDeclaration
case class Datatype(name: String, typeformals: List[String] = Nil, constructors: List[Constructor]) extends TypeDeclaration

//SecurityLevelDeclaration
//For DeclSL in parser
//sealed: class can't be referenced outside of file
//ident is parser that gives String
sealed case class SecurityLevelDeclaration(name: String, parents: List[String], children: List[String]) extends NamedDeclaration


case class Constructor(name: String, types: List[Option[Type]]) extends AST

sealed abstract class Pattern extends AST with OrcSyntaxConvertible {
  val isStrict: Boolean
}

sealed abstract class NonStrictPattern extends Pattern {
  val isStrict = false
}
case class Wildcard() extends NonStrictPattern { override def toOrcSyntax = "_" }
case class VariablePattern(name: String) extends NonStrictPattern { override def toOrcSyntax = name }

sealed abstract class StrictPattern extends Pattern {
  val isStrict = true
}
case class ConstantPattern(c: AnyRef) extends StrictPattern { override def toOrcSyntax = if (c == null) "null" else c.toString }
case class TuplePattern(elements: List[Pattern]) extends StrictPattern { override def toOrcSyntax = elements.map(_.toOrcSyntax).mkString("(", ", ", ")") }
case class ListPattern(elements: List[Pattern]) extends StrictPattern { override def toOrcSyntax = elements.map(_.toOrcSyntax).mkString("[", ", ", "]") }
case class CallPattern(name: String, args: List[Pattern]) extends StrictPattern { override def toOrcSyntax = name + args.map(_.toOrcSyntax).mkString("(", ", ", ")") }
case class ConsPattern(head: Pattern, tail: Pattern) extends StrictPattern { override def toOrcSyntax = "(" + head.toOrcSyntax + ":" + tail.toOrcSyntax + ")" }
case class RecordPattern(elements: List[(String, Pattern)]) extends StrictPattern { override def toOrcSyntax = elements.map({ case (f, p) => f + " = " + p.toOrcSyntax }).mkString("{. ", ", ", " .}") }

//SecurityType pattern
//ST
case class SecurityLevelPattern(p: Pattern, name: String) extends Pattern {   
  val isStrict = p.isStrict
  override def toOrcSyntax = p.toOrcSyntax + " @" + name
  }
		

case class AsPattern(p: Pattern, name: String) extends Pattern {
  
  val isStrict = p.isStrict
  override def toOrcSyntax = p.toOrcSyntax + " as " + name
}
case class TypedPattern(p: Pattern, t: Type) extends Pattern {
  val isStrict = p.isStrict
  override def toOrcSyntax = p.toOrcSyntax + " :: " + t.toOrcSyntax
}

sealed abstract class Type extends AST with OrcSyntaxConvertible

case class TypeVariable(name: String) extends Type { override def toOrcSyntax = name }
case class TupleType(elements: List[Type]) extends Type { override def toOrcSyntax = elements.map(_.toOrcSyntax).mkString("(", ", ", ")") }
case class RecordType(elements: List[(String, Type)]) extends Type { override def toOrcSyntax = elements.map({ case (f, t) => f + " :: " + t.toOrcSyntax }).mkString("{. ", ", ", " .}") }
case class LambdaType(typeformals: List[String], argtypes: List[Type], returntype: Type) extends Type {
  override def toOrcSyntax = "lambda" + (if (typeformals.size > 0) typeformals.mkString("[", ", ", "]") else "") + argtypes.map(_.toOrcSyntax).mkString("(", ", ", ")") + " :: " + returntype.toOrcSyntax

}
case class TypeApplication(name: String, typeactuals: List[Type]) extends Type { override def toOrcSyntax = name + typeactuals.map(_.toOrcSyntax).mkString("[", ", ", "]") }
