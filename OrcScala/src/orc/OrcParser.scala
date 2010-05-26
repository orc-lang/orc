//
// OrcParser.scala -- Scala object OrcParser
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import scala.util.parsing.input.Reader
import scala.util.parsing.combinator.syntactical._

object OrcParser extends StandardTokenParsers {

import ExtendedSyntax._	
	
def parseConstant = (
	"true" ^^^ Constant(true) 
  | "false" ^^^ Constant(false)
  | "signal" ^^^ Constant({})
  | stringLit ^^ Constant
  | numericLit ^^ Constant 
	)
	
def parseVariable: Parser[Variable] = ident ^^ Variable
def parseTypeVariable: Parser[String] = ident

def parseBaseExpression = (
	parseConstant 
  | parseVariable 
  | "stop" ^^^ Stop
  | "(" ~> parseExpression <~ ")"
  | ListOf(parseExpression) ^^ ListExpr
  | TupleOf(parseExpression) ^^ TupleExpr
)

def parseArgumentGroup: Parser[ArgumentGroup] = ( 
    (ListOf(parseType)?) ~ TupleOf(parseExpression) ^^~ Args
  | "." ~> ident ^^ FieldAccess
  | "?" ^^^ Dereference
)

def parseCallExpression: Parser[Expression] = (
	  parseBaseExpression ~ (parseArgumentGroup+) ^^~ Call
	| parseBaseExpression
)

def parseInfixOp = "+" | "-" | "*"
def parseInfixOpExpression: Parser[Expression] = 
		parseCallExpression interleave parseInfixOp apply InfixOperator

		
def parseSequentialCombinator = ">" ~> (parsePattern?) <~ ">"
def parsePruningCombinator = "<" ~> (parsePattern?) <~ "<"

def parseSequentialExpression = 
		parseInfixOpExpression interleave parseSequentialCombinator apply SequentialExpression 

def parseParallelExpression = 
		rep1sep(parseSequentialExpression, "|") ^^ (_ reduceLeft ParallelExpression)
		
def parsePruningExpression = 
		parseParallelExpression interleave parsePruningCombinator apply PruningExpression
		
def parseOtherwiseExpression = 
		rep1sep(parsePruningExpression, ";") ^^ (_ reduceLeft OtherwiseExpression)

		
def parseExpression: Parser[Expression] = (
	"lambda" ~> (ListOf(parseType)?) 
		     ~ TupleOf(parsePattern) 
		     ~ (("::" ~> parseType)?) 
		     ~ ("=" ~> parseExpression)
			 ^^~~~ Lambda
  | ("if" ~> parseExpression)
    ~ ("then" ~> parseExpression) 
    ~ ("else" ~> parseExpression) 
    ^^~~ Conditional
  | parseDeclaration ~ parseExpression ^^~ Declare
  | parseOtherwiseExpression ~ ("::" ~> parseType) ^^~ TypeAscription
  | parseOtherwiseExpression ~ (":!:" ~> parseType) ^^~ TypeAssertion
  | parseOtherwiseExpression
)


def parseBasePattern = (
    parseConstant
  | parseVariable
  | "_" ^^^ Wildcard
  | ident ~ TupleOf(parsePattern) ^^~ CallPattern
  | "(" ~> parsePattern <~ ")"
  | TupleOf(parsePattern) ^^ TuplePattern
  | ListOf(parsePattern) ^^ ListPattern
  | "=" <~ parseVariable ^^ EqPattern
)

def parseConsPattern = rep1sep(parseBasePattern, ":") ^^ (_ reduceLeft ConsPattern)
def parseAsPattern = ( 
	parseConsPattern ~ ("as" ~> ident) ^^~ AsPattern
  | parseConsPattern
)
def parseTypedPattern = (
	parseAsPattern ~ ("::" ~> parseType) ^^~ TypedPattern
  | parseAsPattern
)
def parsePattern: Parser[Pattern] = parseTypedPattern

def parseGroundType = ( 
	"Integer" ^^^ IntegerType
  | "Boolean" ^^^ BooleanType
  | "String" ^^^ StringType
  | "Number" ^^^ NumberType
  | "Signal" ^^^ SignalType
  | "Top" ^^^ Top
  | "Bot" ^^^ Bot
)

def parseType: Parser[Type] = (
	parseGroundType
  | parseTypeVariable ^^ TypeVariable
  | TupleOf(parseType) ^^ TupleType
  | "lambda" ~> ListOf(parseTypeVariable) ~ TupleOf(parseType) ~ parseReturnType ^^~~ FunctionType
  | parseTypeVariable ~ ListOf(parseType) ^^~ TypeApplication
)

def parseConstructor: Parser[Constructor] = (
	ident ~ TupleOf(parseType ^^ (Some(_))) ^^~ Constructor
  | ident ~ TupleOf("_" ^^^ None) ^^~ Constructor
)

def parseReturnType = "::" ~> parseType

def parseClassname = ident 
def parseSitename = ident

def parseDeclaration: Parser[Declaration] = (
	("val" ~> parsePattern) ~ ("=" ~> parseExpression) ^^~ Val
  | ("def" ~> ident) 
  		  ~ TupleOf(parsePattern) 
  		  ~ ("=" ~> parseExpression) 
  		  ~ (parseReturnType?)
  		  ^^~~~ Def
  | "def" ~> ident ~ ListOf(parseTypeVariable) ~ TupleOf(parseType) ~ (parseReturnType?) ^^~~~ DefSig
  | "type" ~> parseTypeVariable ~ ("=" ~> parseClassname) ^^~ TypeImport
  | "type" ~> parseTypeVariable ~ (ListOf(parseTypeVariable)?) ~ ("=" ~> parseType) 
  		^^ { 
  			 case x ~ Some(ys) ~ t => TypeAlias(x,ys,t)
  			 case x ~ None ~ t => TypeAlias(x,Nil,t) 
  		   }
  | "type" ~> parseTypeVariable ~ (ListOf(parseTypeVariable)?) ~ ("=" ~> rep1sep(parseConstructor, "|"))
  		^^ { 
  			 case x ~ Some(ys) ~ t => Datatype(x,ys,t)
  			 case x ~ None ~ t => Datatype(x,Nil,t)
  		   }
  | "site" ~> ident ~ ("=" ~> parseSitename) ^^~ SiteImport
  | "class" ~> ident ~ ("=" ~> parseClassname) ^^~ ClassImport
  | "include" ~> stringLit ^^ Include
)


lexical.delimiters ++= List("+", "-", "*")
lexical.delimiters ++= List("<", ">", "|", ";")
lexical.delimiters ++= List("(", ")", "[", "]", ",", "=")
lexical.delimiters ++= List(":", "_")
lexical.delimiters ++= List(".", "?", ":=")
lexical.delimiters ++= List("::", ":!:")

lexical.reserved ++= List("true", "false", "signal", "stop")
lexical.reserved ++= List("lambda", "if", "then", "else", "as")
lexical.reserved ++= List("val", "def", "type", "site", "class", "include")
lexical.reserved ++= List("Integer", "Boolean", "String", "Number")
lexical.reserved ++= List("Signal", "Top", "Bot")




def parse(s:String) = {
        val tokens = new lexical.Scanner(s)
        phrase(parseExpression)(tokens)
    }

def parse(r:Reader[Char]) = {
        val tokens = new lexical.Scanner(r)
        phrase(parseExpression)(tokens)
    }




// Spec from user guide:
	
//E	::=	 	Expression	 
// 	 	C	 	constant value
// 	|	X	 	variable
// 	|	stop	 	silent expression
// 	|	( E , ... , E )	 	tuple
// 	|	[ E , ... , E ]	 	list
// 	|	E G+	 	call
// 	|	op E	 	prefix operator
// 	|	E op E	 	infix operator
// 	|	E >P> E	 	sequential combinator
// 	|	E | E	 	parallel combinator
// 	|	E <P< E	 	pruning combinator
// 	|	E ; E	 	otherwise combinator
// 	|	lambda ( P , ... , P ) = E	 	closure (untyped)
// 	|	lambda [ T , ... , T ] ( P , ... , P ) :: T = E	 	closure (typed)
// 	|	if E then E else E	 	conditional
// 	|	D E	 	scoped declaration
// 	|	E :: T	 	type ascription
// 	|	E :!: T		type assertion
// 	G	::=	 	Argument group	 
// 	 	( E , ... , E )	 	arguments
// 	|	[ T , ... , T ] ( E , ... , E )	 	type parameters plus arguments
// 	|	. field	 	field access
// 	|	?	 	dereference
//C	::=	Boolean | Number | String | signal | null	Constant	 
//X	::=	identifier	Variable	 
//D	::=	 	Declaration	 
// 	 	val P = E	 	value declaration
// 	|	site X = address	 	site declaration
// 	|	class X = classname	 	class declaration
// 	|	include " filename "	 	inclusion
// 	|	def X( P , ... , P ) = E	 	function declaration
// 	|	def X[ X , ... , X ] ( T , ... , T ) :: T	 	function signature
// 	|	type X = classname	 	type import
// 	|	type X[ X , ... , X ] = T	 	type alias
// 	|	type X = UC | ... | UC	 	datatype declaration (untyped)
// 	|	type X[ X , ... , X ] = TC | ... | TC	 	datatype declaration (typed)
//UC	::=	 X(_, ... ,_)	Constructor (untyped)	 
//TC	::=	 X( T , ... , T )	Constructor (typed)	 
//P	::=	 	Pattern	 
// 	 	X	 	variable
// 	|	C	 	constant
// 	|	_	 	wildcard
// 	|	X ( P , ... , P )	 	datatype pattern
// 	|	( P , ... , P )	 	tuple pattern
// 	|	[ P , ... , P ]	 	list pattern
// 	|	P : P	 	cons pattern
// 	|	P as X	 	as pattern
// 	|	=X	 	equality pattern
// 	|	P :: T	 	type ascription
//T	::=		Type	 
// 	 	X	 	Type variable
// 	|	Integer | Boolean | String | Number | Signal | Top | Bot	 	Ground type
// 	|	( T , ... , T )	 	Tuple type
// 	|	lambda [ X , ... , X ] ( T , ... , T ) :: T	 	Function type
// 	|	X[ T , ... , T ]	 	Type application




// Add helper combinators for ( ... ) and [ ... ] forms
def TupleOf[T](P : => Parser[T]): Parser[List[T]] = "(" ~> repsep(P, ",") <~ ")" 
def ListOf[T](P : => Parser[T]): Parser[List[T]] = "[" ~> repsep(P, ",") <~ "]"	
 

// Add extended apply combinators ^^~ and ^^~~
class OneSquiggleParser[A,B](parser: Parser[~[A,B]]) {
		def ^^~[X](f: (A,B) => X) = parser ^^ { case x ~ y => f(x,y) }	
	}
class TwoSquiggleParser[A,B,C](parser: Parser[~[~[A,B],C]]) {
		def ^^~~[X](f: (A,B,C) => X) = parser ^^ { case x ~ y ~ z => f(x,y,z) }	
	}
class ThreeSquiggleParser[A,B,C,D](parser: Parser[~[~[~[A,B],C],D]]) {
		def ^^~~~[X](f: (A,B,C,D) => X) = parser ^^ { case x ~ y ~ z ~ w => f(x,y,z,w) }	
	}
implicit def CreateOneSquiggleParser[A,B](parser: Parser[~[A,B]]): OneSquiggleParser[A,B] =	new OneSquiggleParser(parser)
implicit def CreateTwoSquiggleParser[A,B,C](parser: Parser[~[~[A,B],C]]): TwoSquiggleParser[A,B,C] = new TwoSquiggleParser(parser)
implicit def CreateThreeSquiggleParser[A,B,C,D](parser: Parser[~[~[~[A,B],C],D]]): ThreeSquiggleParser[A,B,C,D] = new ThreeSquiggleParser(parser)


// Add interleaving combinator
class InterleavingParser[A](parser: Parser[A]) {
		def interleave[B](inter: Parser[B]) = 
		(f: (A,B,A) => A) =>
			{
				def origami(b: B)(x:A, y:A): A = f(x,b,y)
				parser * (inter ^^ origami)
			}
 	}
implicit def CreateInterleavingParser[A](parser: Parser[A]): InterleavingParser[A] = new InterleavingParser(parser)

}