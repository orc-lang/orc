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
  import lexical.{Keyword, FloatingPointLit}

  override val lexical = new OrcLexical()

  def parseClassname: Parser[String] = (
        stringLit
        // For backwards compatibility, allow quotes to be omitted, if class name had only Orc-legal identifier characters
      | rep1sep(ident, ".") ^^ { _.mkString(".") } 
  )

  def floatLit: Parser[String] = 
    elem("number", _.isInstanceOf[FloatingPointLit]) ^^ (_.chars)

  def parseValue: Parser[Any] = (
        "true" ^^^ true 
      | "false" ^^^ false
      | "signal" ^^^ orc.oil.Signal
      | stringLit
      | numericLit ^^ { BigInt(_) }
      | floatLit ^^ { BigDecimal(_) }
      | "null" ^^^ null
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

  def parseUnaryExpr = "-" ~ parseCallExpression ^^ { case op ~ expr => PrefixOperator(op, expr)} |
    parseCallExpression

  def parseExpnExpr = chainl1(parseUnaryExpr, ("**") ^^ 
    { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })

  def parseMultExpr = chainl1(parseExpnExpr, ("*" | "/" | "%") ^^ 
    { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })
  
  def parseAdditionalExpr: Parser[Expression] = chainl1(parseMultExpr, ("+" | "-" | ":") ^^ 
    { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) }) | 
    "~" ~ parseAdditionalExpr ^^ { case op ~ expr => PrefixOperator(op, expr)}
  
  def parseRelationalExpr = chainl1(parseAdditionalExpr, ("=" | "/=" | "<" | ">" | "<=" | ">=") ^^
        { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })

  def parseLogicalExpr = chainl1(parseRelationalExpr, ("||" | "&&") ^^
   { op =>(left:Expression,right:Expression) => InfixOperator(left, op, right)})

  def parseInfixOpExpression: Parser[Expression] = chainl1(parseLogicalExpr, ":=" ^^ 
    { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })

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

/*---------------------------------------------------------
Expressions

For reference, here are Orc's operators sorted
and grouped in order of increasing precedence.

Symbol   Assoc       Name
----------------------------------------------------
 lambda  prefix[3]   anonymous function
 if      prefix[3]   if/then/else
----------------------------------------------------
 ;       left        semicolon
----------------------------------------------------
 <<      left        where
----------------------------------------------------
 |       right[2]    parallel
----------------------------------------------------
 >>      right       sequence
----------------------------------------------------
 ::      right       type annotation
 :=      none        ref assignment
----------------------------------------------------
 ||      right[2]    boolean or
 &&      right[2]    boolean and
----------------------------------------------------
 =       none        equal
 /=      none        not equal
 :>      none        greater
 >       none[1]     greater
 >=      none        greater or equal
 <:      none        less
 <       none[1]     less
 <=      none        less or equal
----------------------------------------------------
 ~       prefix      boolean not
 :       right       cons
 +       left        addition
 -       left[4]     subtraction
----------------------------------------------------
 *       left        multiplication
 /       left        division
 %       left        modulus
----------------------------------------------------
 **      left        exponent
----------------------------------------------------
 -       prefix      unary minus
----------------------------------------------------
 ?       postfix     dereference
 .       postfix     field projection
 ()      postfix[4]  application

[1] An expression like (a > b > c > d) could be read as
either ((a >b> c) > d) or (a > (b >c> d)). We could resolve
this ambiguity with precedence, but that's likely to
violate the principle of least suprise. So instead we just
disallow these operators unparenthesized inside << or >>.
This results in some unfortunate duplication of the lower-
precedence operators: see the NoCmp_ productions.

[2] These operators are semantically fully associative, but
they are implemented as right-associative because it's
slightly more efficient in Rats!.

[3] I'm not sure whether I'd call lambda and if operators,
but the point is they bind very loosely, so everything to
their right is considered part of their body.

[4] These operators may not be preceded by a newline, to
avoid ambiguity about where an expression ends.  So if you
have a newline in front of a minus, the parser assumes
it's a unary minus, and if you have a newline in front of
a paren, the parser assumes it's a tuple (as opposed to a
function application).  Hopefully these rules match how
people intuitively use these operators.
-----------------------------------------------------------*/

  
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

  def parseType: Parser[Type] = (
        "Top" -> Top
      | "Bot" -> Bot
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

  def parseDeclaration: Parser[Declaration] = (
      ("val" ~> parsePattern) ~ ("=" ~> parseExpression) 
      -> Val

      | ("def" ~> ident) ~ TupleOf(parsePattern) ~ ("=" ~> parseExpression) ~ (parseReturnType?) 
      -> Def

      | ("def" ~> "capsule" ~> ident) ~ TupleOf(parsePattern) ~ ("=" ~> parseExpression) ~ (parseReturnType?) 
      -> DefCapsule

      | "def" ~> ident ~ (ListOf(parseTypeVariable)?) ~ TupleOf(parseType) ~ (parseReturnType?) 
      -> { (id, tvs, ts, rt) => DefSig(id, tvs getOrElse Nil, ts, rt) }

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

  def parseInclude(options: OrcOptions, r:Reader[Char], name: String) = {
      val parseInclude = phrase(parseDeclaration*) -> { Include(name, _) } 
      val tokens = new lexical.Scanner(r)
      phrase(parseInclude)(tokens)
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
  class Maps2[A,B](parser: Parser[A ~ B]) {
    def ->[X <: AST](f: (A,B) => X): Parser[X] = 
      markLocation(parser ^^ { case x ~ y => f(x,y) })	
  }
  class Maps3[A,B,C](parser: Parser[A ~ B ~ C]) {
    def ->[X <: AST](f: (A,B,C) => X): Parser[X] = 
      markLocation(parser ^^ { case x ~ y ~ z => f(x,y,z) })	
  }
  class Maps4[A,B,C,D](parser: Parser[A ~ B ~ C ~ D]) {
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
  implicit def CreateMaps2Parser[A,B](parser: Parser[A ~ B]): Maps2[A,B] =	new Maps2(parser)
  implicit def CreateMaps3Parser[A,B,C](parser: Parser[A ~ B ~ C]): Maps3[A,B,C] = new Maps3(parser)
  implicit def CreateMaps4Parser[A,B,C,D](parser: Parser[A ~ B ~ C ~ D]): Maps4[A,B,C,D] = new Maps4(parser)
  implicit def CreateInterleavingParser[A <: AST](parser: Parser[A]): InterleavingParser[A] = new InterleavingParser(parser)

}
