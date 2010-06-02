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
import scala.util.parsing.input.CharArrayReader.EofCh
import scala.util.parsing.combinator.syntactical._
import orc.ext._

object OrcParser extends StandardTokenParsers {
  import lexical.{FloatingPointLit}

  override val lexical = new OrcLexical()

  def floatLit: Parser[String] = 
    elem("number", _.isInstanceOf[FloatingPointLit]) ^^ (_.chars)

  def parseValue: Parser[Any] = (
        "true" ^^^ true 
      | "false" ^^^ false
      | "signal" ^^^ {}
      | stringLit
      | numericLit ^^ { BigInt(_) }
      | floatLit ^^ { BigDecimal(_) }
  )

  def parseConstant = parseValue -> Constant

  def parseTypeVariable: Parser[String] = ident

  def parseBaseExpression = (
      parseValue -> Constant 
      | ident -> Variable 
      | "stop" -> Stop
      | "(" ~> parseExpression <~ ")"
      | ListOf(parseExpression) -> ListExpr
      | TupleOf(parseExpression) -> TupleExpr
  )

  def parseArgumentGroup: Parser[ArgumentGroup] = ( 
      (ListOf(parseType)?) ~ TupleOf(parseExpression) -> Args
      | "." ~> ident -> FieldAccess
      | "?" -> Dereference
  )

  def parseCallExpression: Parser[Expression] = (
      parseBaseExpression ~ (parseArgumentGroup+) -> Call
      | parseBaseExpression
  )

  def parseInfixOp = "+" | "-" | "*" 

  def parseInfixOpExpression: Parser[Expression] = 
    parseCallExpression interleave parseInfixOp apply InfixOperator


  def parseSequentialCombinator = ">" ~> (parsePattern?) <~ ">"
 
  def parsePruningCombinator = "<" ~> (parsePattern?) <~ "<"

  def parseSequentialExpression = 
    parseInfixOpExpression interleave parseSequentialCombinator apply Sequential 

  def parseParallelExpression = 
    rep1sep(parseSequentialExpression, "|") -> (_ reduceLeft Parallel)

  def parsePruningExpression = 
    parseParallelExpression interleave parsePruningCombinator apply Pruning

  def parseOtherwiseExpression = 
    rep1sep(parsePruningExpression, ";") -> (_ reduceLeft Otherwise)

  def parseExpression: Parser[Expression] = (
      "lambda" ~> (ListOf(parseType)?) 
      ~ TupleOf(parsePattern) 
      ~ (("::" ~> parseType)?) 
      ~ ("=" ~> parseExpression)
      -> Lambda
      | ("if" ~> parseExpression)
      ~ ("then" ~> parseExpression) 
      ~ ("else" ~> parseExpression)
      -> Conditional
      | parseDeclaration ~ parseExpression -> Declare
      | parseOtherwiseExpression ~ ("::" ~> parseType) -> TypeAscription
      | parseOtherwiseExpression ~ (":!:" ~> parseType) -> TypeAssertion
      | parseOtherwiseExpression
  )

  def parseBasePattern = (
      parseValue -> ConstantPattern
      | ident -> VariablePattern
      | "_" -> Wildcard
      | ident ~ TupleOf(parsePattern) -> CallPattern
      | "(" ~> parsePattern <~ ")"
      | TupleOf(parsePattern) -> TuplePattern
      | ListOf(parsePattern) -> ListPattern
      | ("=" <~ ident) -> EqPattern
  )

  def parseConsPattern = rep1sep(parseBasePattern, ":") -> (_ reduceLeft ConsPattern)

  def parseAsPattern = ( 
      parseConsPattern ~ ("as" ~> ident) -> AsPattern
      | parseConsPattern
  )

  def parseTypedPattern = (
      parseAsPattern ~ ("::" ~> parseType) -> TypedPattern
      | parseAsPattern
  )

  def parsePattern: Parser[Pattern] = parseTypedPattern

  def parseGroundType: Parser[Type] = ( 
      ("Integer" | "Boolean" | "String" | "Number" | "Signal") -> NativeType
      | "Top" -> Top
      | "Bot" -> Bot
  )

  def parseType: Parser[Type] = (
      parseGroundType
      | parseTypeVariable -> TypeVariable
      | TupleOf(parseType) -> TupleType
      | "lambda" ~> ListOf(parseTypeVariable) ~ TupleOf(parseType) ~ parseReturnType -> FunctionType
      | parseTypeVariable ~ ListOf(parseType) -> TypeApplication
  )

  def parseConstructor: Parser[Constructor] = (
      ident ~ TupleOf(parseType ^^ (Some(_))) -> Constructor
      | ident ~ TupleOf("_" ^^^ None) -> Constructor
  )

  def parseReturnType = "::" ~> parseType

  def parseClassname: Parser[String] =
      ( stringLit
        // For backwards compatibility, allow quotes to be omitted, if class name had only Orc-legal identifier characters
      | rep1sep(ident, ".") ^^ { _.mkString(".") } 
      )

  def parseDeclaration: Parser[Declaration] = (
      ("val" ~> parsePattern) ~ ("=" ~> parseExpression) 
      -> Val

      | ("def" ~> ident) ~ TupleOf(parsePattern) ~ ("=" ~> parseExpression) ~ (parseReturnType?) 
      -> Def

      | ("def" ~> "capsule" ~> ident) ~ TupleOf(parsePattern) ~ ("=" ~> parseExpression) ~ (parseReturnType?) 
      -> DefCapsule

      | "def" ~> ident ~ ListOf(parseTypeVariable) ~ TupleOf(parseType) ~ (parseReturnType?) 
      -> DefSig

      | "type" ~> parseTypeVariable ~ ("=" ~> parseClassname) 
      -> TypeImport

      | "type" ~> parseTypeVariable ~ (ListOf(parseTypeVariable)?) ~ ("=" ~> parseType) 
      -> ((x,ys,t) => TypeAlias(x, ys getOrElse Nil, t))

      | "type" ~> parseTypeVariable ~ (ListOf(parseTypeVariable)?) ~ ("=" ~> rep1sep(parseConstructor, "|"))
      -> ((x,ys,t) => Datatype(x, ys getOrElse Nil, t))

      | "site" ~> ident ~ ("=" ~> parseClassname) 
      -> SiteImport

      | "class" ~> ident ~ ("=" ~> parseClassname) 
      -> ClassImport

      | "include" ~> stringLit 
      -> { Include(_ : String, Nil) }
  )

  // Add helper combinators for ( ... ) and [ ... ] forms
  def TupleOf[T](P : => Parser[T]): Parser[List[T]] = "(" ~> repsep(P, ",") <~ ")" 
  def ListOf[T](P : => Parser[T]): Parser[List[T]] = "[" ~> repsep(P, ",") <~ "]"	


  def parse(options: OrcOptions, s:String) = {
      val tokens = new lexical.Scanner(s)
      phrase(parseExpression)(tokens)
  }

  def parse(options: OrcOptions, r:Reader[Char]) = {
      val tokens = new lexical.Scanner(r)
      phrase(parseExpression)(tokens)
  }


  class LocatingParser[+A <: AST](p : => Parser[A]) extends Parser[A] {
    override def apply(i: Input) = {
      val position = i.pos
      val result: ParseResult[A] = p.apply(i)
      result map { _.pos = position }
      result
    }
  }
  def markLocation[A <: AST](p : => Parser[A]) = new LocatingParser(p)



  // Add extended apply combinator ->
  class Maps0(s: String) {
    def ->[A <: AST](a: () => A): Parser[A] = {
        markLocation(keyword(s) ^^^ a())
    }
    def ->[A <: AST](a: A): Parser[A] = {
        markLocation(keyword(s) ^^^ a)
    }
  }
  class Maps1[A](parser: Parser[A]) {
    def ->[X <: AST](f: A => X): Parser[X] = {
        markLocation(parser ^^ f)
    }
  }
  class Maps2[A,B](parser: Parser[~[A,B]]) {
    def ->[X <: AST](f: (A,B) => X): Parser[X] = 
      markLocation(parser ^^ { case x ~ y => f(x,y) })	
  }
  class Maps3[A,B,C](parser: Parser[~[~[A,B],C]]) {
    def ->[X <: AST](f: (A,B,C) => X): Parser[X] = 
      markLocation(parser ^^ { case x ~ y ~ z => f(x,y,z) })	
  }
  class Maps4[A,B,C,D](parser: Parser[~[~[~[A,B],C],D]]) {
    def ->[X <: AST](f: (A,B,C,D) => X): Parser[X] = 
      markLocation(parser ^^ { case x ~ y ~ z ~ w => f(x,y,z,w) })	
  }

  // Add interleaving combinator
  class InterleavingParser[A <: AST](parser: Parser[A]) {
    def interleave[B](interparser: Parser[B]) = 
      (f: (A,B,A) => A) =>
    {

      def origami(b: B)(x:A, y:A): A = f(x,b,y)
      markLocation( (markLocation(parser)) * (interparser ^^ origami) ) 

    } : Parser[A]
  }

  implicit def CreateMaps0Parser(s: String): Maps0 = new Maps0(s)
  implicit def CreateMaps1Parser[A](parser: Parser[A]): Maps1[A] = new Maps1(parser)
  implicit def CreateMaps2Parser[A,B](parser: Parser[~[A,B]]): Maps2[A,B] =	new Maps2(parser)
  implicit def CreateMaps3Parser[A,B,C](parser: Parser[~[~[A,B],C]]): Maps3[A,B,C] = new Maps3(parser)
  implicit def CreateMaps4Parser[A,B,C,D](parser: Parser[~[~[~[A,B],C],D]]): Maps4[A,B,C,D] = new Maps4(parser)
  implicit def CreateInterleavingParser[A <: AST](parser: Parser[A]): InterleavingParser[A] = new InterleavingParser(parser)

}
