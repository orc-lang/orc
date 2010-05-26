//
// Extended.scala -- Scala object ExtendedSyntax
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 20, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

object ExtendedSyntax {
	
	trait Expression
	
	case object Stop extends Expression
	case object Signal extends Expression
	case class Constant(c: Any) extends Expression with Pattern
	case class Variable(name: String) extends Expression with Pattern
	case class TupleExpr(elements: List[Expression]) extends Expression
	case class ListExpr(elements: List[Expression]) extends Expression
	case class Call(target: Expression, gs: List[ArgumentGroup]) extends Expression
		trait ArgumentGroup
		case class Args(types: Option[List[Type]] = None, elements: List[Expression]) extends ArgumentGroup	 
		case class FieldAccess(field: String) extends ArgumentGroup
		case object Dereference extends ArgumentGroup
	case class PrefixOperator(op: String, arg: Expression) extends Expression
	case class InfixOperator(left: Expression, op: String, right: Expression) extends Expression
	case class SequentialExpression(left: Expression, p: Option[Pattern] = None, right: Expression) extends Expression
	case class ParallelExpression(left: Expression, right: Expression) extends Expression
	case class PruningExpression(left: Expression, p: Option[Pattern] = None, right: Expression) extends Expression
	case class OtherwiseExpression(left: Expression, right: Expression) extends Expression
	case class Lambda(typeformals: Option[List[Type]] = None, 
					  formals: List[Pattern],
					  returntype: Option[Type] = None,
					  body: Expression
					  ) extends Expression
	
	case class Conditional(ifE: Expression, thenE: Expression, elseE: Expression) extends Expression
	case class Declare(declaration: Declaration, body: Expression) extends Expression
	case class TypeAscription(e: Expression, t: Type) extends Expression
	case class TypeAssertion(e: Expression, t: Type) extends Expression
	
	
	
	trait Declaration
	
	case class Val(p: Pattern, e: Expression) extends Declaration
	// to add to user guide: def is allowed to have optional inline return type
	case class Def(name: String, formals: List[Pattern], body: Expression, returntype: Option[Type]) extends Declaration
	case class DefSig(name: String, typeformals: List[String], argtypes: List[Type], returntype: Option[Type]) extends Declaration
	case class TypeAlias(name: String, typeformals: List[String] = Nil, aliasedtype: Type) extends Declaration
	case class Datatype(name: String, typeformals: List[String] = Nil, constructors: List[Constructor]) extends Declaration
		case class Constructor(name: String, types: List[Option[Type]]) extends Declaration
	case class TypeImport(name: String, classname: String) extends Declaration
	case class SiteImport(name: String, sitename: String) extends Declaration
	case class ClassImport(name: String, classname: String) extends Declaration
	case class Include(filename: String) extends Declaration
	
	
	
	trait Pattern
	// Constant
	// Variable
	case object Wildcard extends Pattern
	case class TuplePattern(elements: List[Pattern]) extends Pattern
	case class ListPattern(elements: List[Pattern]) extends Pattern
	case class CallPattern(name: String, args: List[Pattern]) extends Pattern
	case class ConsPattern(head: Pattern, tail: Pattern) extends Pattern
	case class AsPattern(p: Pattern, name: String) extends Pattern
	case class EqPattern(name: String) extends Pattern
	case class TypedPattern(p: Pattern, t: Type) extends Pattern
	
		
	
	trait Type
	case object IntegerType extends Type
	case object BooleanType extends Type
	case object StringType extends Type
	case object NumberType extends Type
	case object SignalType extends Type
	case object Top extends Type
	case object Bot extends Type
	case class TypeVariable(name: String) extends Type
	case class TupleType(elements: List[Type]) extends Type
	case class FunctionType(typeformals: List[String], argtypes: List[Type], returntype: Type) extends Type
	case class TypeApplication(name: String, typeactuals: List[Type]) extends Type	
	
}