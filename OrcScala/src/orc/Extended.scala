package orc.ext

import orc.AST

abstract class Expression extends AST

case class Stop extends Expression
case class Constant(c: Any) extends Expression
case class Variable(name: String) extends Expression
case class TupleExpr(elements: List[Expression]) extends Expression
case class ListExpr(elements: List[Expression]) extends Expression
case class Call(target: Expression, gs: List[ArgumentGroup]) extends Expression

abstract class ArgumentGroup extends AST
case class Args(types: Option[List[Type]] = None, elements: List[Expression]) extends ArgumentGroup	 
case class FieldAccess(field: String) extends ArgumentGroup
case class Dereference extends ArgumentGroup

case class PrefixOperator(op: String, arg: Expression) extends Expression
case class InfixOperator(left: Expression, op: String, right: Expression) extends Expression
case class Sequential(left: Expression, p: Option[Pattern] = None, right: Expression) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Pruning(left: Expression, p: Option[Pattern] = None, right: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression
case class Lambda(
    typeformals: Option[List[Type]] = None, 
    formals: List[Pattern],
    returntype: Option[Type] = None,
    body: Expression
) extends Expression

case class Conditional(ifE: Expression, thenE: Expression, elseE: Expression) extends Expression
case class Declare(declaration: Declaration, body: Expression) extends Expression
case class TypeAscription(e: Expression, t: Type) extends Expression
case class TypeAssertion(e: Expression, t: Type) extends Expression



abstract class Declaration extends AST
// to add to user guide: def is allowed to have optional inline return type
abstract class DefDeclaration extends Declaration
case class Def(name: String, formals: List[Pattern], body: Expression, returntype: Option[Type]) extends DefDeclaration
case class DefSig(name: String, typeformals: List[String], argtypes: List[Type], returntype: Option[Type]) extends DefDeclaration

abstract class TypeDeclaration extends Declaration
case class TypeAlias(name: String, typeformals: List[String] = Nil, aliasedtype: Type) extends Declaration
case class Datatype(name: String, typeformals: List[String] = Nil, constructors: List[Constructor]) extends Declaration
case class Constructor(name: String, types: List[Option[Type]]) extends AST
case class TypeImport(name: String, classname: String) extends Declaration

abstract class SiteDeclaration extends Declaration
case class SiteImport(name: String, sitename: String) extends Declaration
case class ClassImport(name: String, classname: String) extends Declaration
case class Val(p: Pattern, e: Expression) extends Declaration
case class Include(filename: String) extends Declaration




abstract class Pattern extends AST

case class Wildcard extends Pattern
case class ConstantPattern(c: Any) extends Pattern
case class VariablePattern(name: String) extends Pattern
case class TuplePattern(elements: List[Pattern]) extends Pattern
case class ListPattern(elements: List[Pattern]) extends Pattern
case class CallPattern(name: String, args: List[Pattern]) extends Pattern
case class ConsPattern(head: Pattern, tail: Pattern) extends Pattern
case class AsPattern(p: Pattern, name: String) extends Pattern
case class EqPattern(name: String) extends Pattern
case class TypedPattern(p: Pattern, t: Type) extends Pattern


abstract class Type extends AST

case class Top extends Type
case class Bot extends Type
case class NativeType(name: String) extends Type
case class TypeVariable(name: String) extends Type
case class TupleType(elements: List[Type]) extends Type
case class FunctionType(typeformals: List[String], argtypes: List[Type], returntype: Type) extends Type
case class TypeApplication(name: String, typeactuals: List[Type]) extends Type	
