//
// OrcParsers.scala -- Scala objects Orc...Parser, and class OrcParsers
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

package orc.compile.parse

import scala.util.parsing.input.Reader
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.input.Position

import java.io.IOException

import orc.ast.AST
import orc.OrcCompilationOptions
import orc.OrcCompilerRequires
import orc.ast.ext._


/**
 * Mix-in for result types from Orc parsers
 *
 * @author jthywiss
 */
trait OrcParserResultTypes[T] {
  type ResultType = T
  type ParseResult = OrcParsers#ParseResult[T]
  type ParseResultT[U] = OrcParsers#ParseResult[U]
  type Success = OrcParsers#Success[T]
  type SuccessT[U] = OrcParsers#Success[U]
  type NoSuccess = OrcParsers#NoSuccess
  type Error =  OrcParsers#Error
  type Failure = OrcParsers#Failure
}


/**
 * An Orc parser that analyzes a given string as an Orc literal value.
 * The result of applying this function is an Expression Extended AST,
 * in particular a Constant, ListValue, or TupleValue.
 *
 * @see orc.ast.ext.Expression
 * @see orc.ast.ext.Constant
 * @see orc.ast.ext.ListValue
 * @see orc.ast.ext.TupleValue
 * @author jthywiss
 */
object OrcLiteralParser extends (String => OrcParsers#ParseResult[Expression]) with OrcParserResultTypes[Expression] {
  def apply(s: String): ParseResult = {
    val parsers = new OrcParsers(null, null, null)
    val tokens = new parsers.lexical.Scanner(s)
    parsers.phrase(parsers.parseConstantListTuple)(tokens)
  }
}

/**
 * An Orc parser that analyzes a given input as an Orc program.
 * The result of applying this function is an Expression Extended AST
 * representing the whole program and included files.
 *
 * @author jthywiss
 */
object OrcProgramParser extends ((OrcInputContext, OrcCompilationOptions, OrcCompilerRequires) => OrcParsers#ParseResult[Expression]) with OrcParserResultTypes[Expression] {
  def apply(ic: OrcInputContext, options: OrcCompilationOptions, envServices: OrcCompilerRequires): ParseResult = {
    val parsers = new OrcParsers(ic, options, envServices)
    val tokens = new parsers.lexical.Scanner(ic.reader)
    parsers.enhanceErrorMsg(parsers.phrase(parsers.parseProgram)(tokens))
  }
}

/**
 * An Orc parser that analyzes a given input as an Orc include file.
 * The result of applying this function is an Include Extended AST
 * representing the include and sub-included files.
 *
 * @author jthywiss
 */
object OrcIncludeParser extends ((OrcInputContext, OrcCompilationOptions, OrcCompilerRequires) => OrcParsers#ParseResult[Include]) with OrcParserResultTypes[Include] {
  def apply(ic: OrcInputContext, options: OrcCompilationOptions, envServices: OrcCompilerRequires): ParseResult = {
    val newParsers = new OrcParsers(ic, options, envServices)
    val parseInclude = newParsers.markLocation(newParsers.parseDeclarations ^^ { Include(ic.descr, _) })
    val tokens = new newParsers.lexical.Scanner(ic.reader)
    newParsers.phrase(parseInclude)(tokens)
  }
}


/**
 * This is a container for the various Parsers that embody the Orc grammar
 * productions, and some parsing helper methods.
 * <p>
 * A fresh OrcParsers instance is needed for every parse.
 * (Limitation of scala.util.parsing.combinator.Parsers -- current parsing state kept in fields.)
 *
 * @author dkitchin, amshali, srosario, jthywiss
 */
class OrcParsers(inputContext: OrcInputContext, options: OrcCompilationOptions, envServices: OrcCompilerRequires) 
extends StandardTokenParsers 
with EnhancedErrorMessages
with CustomParserCombinators
{
  import lexical.{Keyword, Identifier, FloatingPointLit}

  override val lexical = new OrcLexical()


/*---------------------------------------------------------
  For reference, here are Orc's operators sorted
  and grouped in order of decreasing precedence.
  
  When an operator's associativity is "both",
  the parser currently defaults to "left".
  
  Symbol   Assoc       Name
  ----------------------------------------------------
                 [highest precedence]
  ----------------------------------------------------
   ?       postfix     dereference
   .       postfix     field projection
   ()      postfix     application
  ----------------------------------------------------
   ~       prefix      boolean not
   -       prefix      unary minus 
  ----------------------------------------------------
   **      left        exponent 
  ----------------------------------------------------
   *       left        multiplication
   /       left        division
   %       left        modulus 
  ----------------------------------------------------
   +       left        addition
   -       left        subtraction 
  ----------------------------------------------------
   :       right       cons 
  ----------------------------------------------------
   =       none        equal
   /=      none        not equal
   :>      none        greater
   >       none        greater
   >=      none        greater or equal
   <:      none        less
   <       none        less
   <=      none        less or equal
  ----------------------------------------------------
   ||      both        boolean or
   &&      both        boolean and
  ----------------------------------------------------
   :=      none        ref assignment 
  ----------------------------------------------------
   >>      right       sequence 
  ----------------------------------------------------
   |       both        parallel  
  ----------------------------------------------------
   <<      left        where 
  ----------------------------------------------------
   ;       both        semicolon 
  ----------------------------------------------------
   ::      left        type ascription
   :!:     left        type assertion
   lambda  prefix      anonymous function
  ----------------------------------------------------
                 [lowest precedence]
  ----------------------------------------------------*/
  

  
  
  
  
/* 
 * Here are the productions of the Orc grammar, 
 * given in the following order:
 * 
 * Literals
 * Expressions
 * Patterns
 * Types
 * Declarations 
 * 
 */
  



/* Literals */
  
  val floatLit: Parser[String] =
    elem("number", _.isInstanceOf[FloatingPointLit]) ^^ (_.chars)

  val parseValue: Parser[AnyRef] = (
        "true" ^^^ java.lang.Boolean.TRUE
      | "false" ^^^ java.lang.Boolean.FALSE
      | "signal" ^^^ orc.values.Signal
      | stringLit
      | numericLit ^^ { BigInt(_) }
      | floatLit ^^ { BigDecimal(_) }
      | "null" ^^^ null
  )

  val parseSiteLocation = stringLit

  
  
  
  
  
/* Expressions */
  
  val parseBaseExpressionTail: Parser[Option[List[Expression]]] = (
        ")" ^^^ None
      | "," ~> CommaSeparated1(parseExpression) <~ ")" ^^ {Some(_)}
  )

  val parseBaseExpression = (
        parseValue -> Constant
      | ident -> Variable
      | "stop" -> Stop
      | ListOf(parseExpression) -> ListExpr
      | RecordOf("=", parseExpression) -> RecordExpr
      | ("(" ~> parseExpression ~ parseBaseExpressionTail) -?->
            { (e: Expression, es: List[Expression]) => TupleExpr(e::es) }
  )

  val parseArgumentGroup: Parser[ArgumentGroup] = (
      (("[" ~> CommaSeparated(parseType) <~ "]")?) ~ ("(" ~> CommaSeparated(parseExpression) <~ ")") -> Args
      | "." ~> ident -> FieldAccess
      | "?" -> Dereference
  )

  val parseCallExpression: Parser[Expression] = (
        parseBaseExpression ~ ((parseArgumentGroup+)?) -?-> Call
  )

  val parseConditionalExpression: Parser[Expression] = (
        ("if" ~> parseOtherwiseExpression)
        ~ ("then" ~> parseOtherwiseExpression)
        ~ ("else" ~> parseOtherwiseExpression)
        -> Conditional
    |
      parseCallExpression
  )

  val parseUnaryExpr: Parser[Expression] = (
    // First see if it's a unary minus for a numeric literal
      "-" ~> numericLit -> { s => Constant(-BigInt(s)) }
    | "-" ~> floatLit -> { s => Constant(-BigDecimal(s)) }
    | ("-" | "~") ~ parseConditionalExpression -> PrefixOperator
    | parseConditionalExpression
    )

  def parseExpnExpr          = parseUnaryExpr      rightAssociativeInfix  List("**")
  def parseMultExpr          = parseExpnExpr       leftAssociativeInfix  List("*", "/", "%")
  def parseAdditionalExpr    = parseMultExpr       leftAssociativeInfix  List("-", "+")
  def parseConsExpr          = parseAdditionalExpr rightAssociativeInfix List(":")
  def parseRelationalExpr    = parseConsExpr       nonAssociativeInfix   List("<:", ":>", "<=", ">=", "=", "/=")
  def parseLogicalExpr       = parseRelationalExpr fullyAssociativeInfix List("||", "&&")
  def parseInfixOpExpression = parseLogicalExpr    nonAssociativeInfix   List(":=")

  val parseSequentialCombinator = ">" ~> (parsePattern?) <~ ">"
  val parsePruningCombinator = "<" ~> (parsePattern?) <~ "<"

  val parseSequentialExpression =
    parseInfixOpExpression rightInterleave parseSequentialCombinator apply Sequential

  val parseParallelExpression =
    rep1sep(parseSequentialExpression, "|") -> { _ reduceLeft Parallel }

  val parsePruningExpression =
    parseParallelExpression leftInterleave parsePruningCombinator apply Pruning

  val parseOtherwiseExpression =
    rep1sep(parsePruningExpression, ";") -> { _ reduceLeft Otherwise }

  val parseAscription = (
        ("::" ~ parseType)
      | (":!:" ~ parseType)
  )

  val parseExpression: Parser[Expression] = (
        "lambda" ~> (ListOf(parseTypeVariable)?)
        ~ (TupleOf(parsePattern))
        ~ (parseReturnType?)
        ~ (parseGuard?)
        ~ ("=" ~> parseExpression)
        -> Lambda
      | parseDeclaration ~ parseExpression -> Declare
      | parseOtherwiseExpression ~ (parseAscription?) -?->
        { (_,_) match
          {
            case (e, "::" ~ t) => TypeAscription(e,t)
            case (e, ":!:" ~ t) => TypeAssertion(e,t)
          }
        }
  )


  
  
  
  
/* Patterns */

  val parseBasePatternTail: Parser[Option[List[Pattern]]] = (
        ")" ^^^ None
      | "," ~> CommaSeparated(parsePattern) <~ ")" ^^ {Some(_)}
  )

  val parseBasePattern = (
      parseValue -> ConstantPattern
      | "_" -> Wildcard
      | ident ~ TupleOf(parsePattern) -> CallPattern
      | ident -> VariablePattern
      | ("(" ~> parsePattern ~ parseBasePatternTail) -?->
          { (p: Pattern, ps: List[Pattern]) => TuplePattern(p::ps) }
      | ListOf(parsePattern) -> ListPattern
      | RecordOf("=", parsePattern) -> RecordPattern
  )

  val parseConsPattern = rep1sep(parseBasePattern, ":") -> (_ reduceRight ConsPattern)

  val parseAsPattern = (
      parseConsPattern ~ (("as" ~> ident)?) -?-> AsPattern
  )

  val parseTypedPattern = (
      parseAsPattern ~ (("::" ~> parseType)?) -?-> TypedPattern
  )

  val parsePattern: Parser[Pattern] = parseTypedPattern

  
  
  
  
  
  
  
/* Types */
    
  val parseTypeVariable: Parser[String] = ident

  
  val parseTypeTail: Parser[Option[List[Type]]] = (
        ")" ^^^ None
      | "," ~> CommaSeparated1(parseType) <~ ")" ^^ {Some(_)}
  )
  
  val parseType: Parser[Type] = (
        parseTypeVariable ~ (ListOf(parseType)?) ->
        { (_,_) match
          {
            case (id, None) => TypeVariable(id)
            case (id, Some(ts)) => TypeApplication(id, ts)
          }
        }
      | ("(" ~> parseType ~ parseTypeTail) -?->
          { (t: Type, ts: List[Type]) => TupleType(t::ts) }
      | TupleOf(parseType) -> TupleType
      | RecordOf("::", parseType) -> RecordType
      | "lambda" ~> ((ListOf(parseTypeVariable)?) ^^ {_.getOrElse(Nil)}) ~ (TupleOf(parseType)) ~ parseReturnType -> LambdaType
  )
  
  val parseConstructor: Parser[Constructor] = (
      ident ~ TupleOf(parseType ^^ (Some(_))) -> Constructor
      | ident ~ TupleOf("_" ^^^ None) -> Constructor
  )
  
  
  
/* Declarations */

  val parseDefDeclaration: Parser[DefDeclaration] = (
        ident ~ (ListOf(parseTypeVariable)?) ~ (TupleOf(parsePattern)) ~ (parseReturnType?) ~ (parseGuard?) ~ ("=" ~> parseExpression)
      -> Def
      
      | (Identifier("class") ~> ident) ~ (ListOf(parseTypeVariable)?) ~ (TupleOf(parsePattern)) ~ (parseReturnType?) ~ (parseGuard?) ~ ("=" ~> parseExpression)
      -> DefClass
      
      | ident ~ (ListOf(parseTypeVariable)?) ~ (TupleOf(parseType)) ~ parseReturnType 
      -> DefSig
  )

  val parseDeclaration: Parser[Declaration] = (
      ("val" ~> parsePattern) ~ ("=" ~> parseExpression)
      -> Val

      | "def" ~> parseDefDeclaration

      | "import" ~> Identifier("site") ~> ident ~ ("=" ~> parseSiteLocation) -> SiteImport

      | "import" ~> Identifier("class") ~> ident ~ ("=" ~> parseSiteLocation) -> ClassImport

      | ("include" ~> stringLit).into(performInclude)

      | "import" ~> "type" ~> ident ~ ("=" ~> parseSiteLocation) -> TypeImport

      | "type" ~> parseTypeVariable ~ ((ListOf(parseTypeVariable))?) ~ ("=" ~> rep1sep(parseConstructor, "|"))
      -> ((x,ys,t) => Datatype(x, ys getOrElse Nil, t))

      | "type" ~> parseTypeVariable ~ ((ListOf(parseTypeVariable))?) ~ ("=" ~> parseType)
      -> ((x,ys,t) => TypeAlias(x, ys getOrElse Nil, t))

      | failure("Declaration (val, def, type, etc.) expected")
  )  
  
  def performInclude(includeName: String): Parser[Include] = {
    val newInputContext = try {
      envServices.openInclude(includeName, inputContext, options)
    } catch {
      case e: IOException => return err(e.toString)
    }
    OrcIncludeParser(newInputContext, options, envServices) match {
      case r: OrcIncludeParser.SuccessT[_] => success(r.get)
      case n: OrcIncludeParser.NoSuccess   => Parser{ in => Error(n.msg, new Input{ def first = null; def rest = this; def pos = n.next.pos; def atEnd = true }) }
    }
  }

  
  
  
  
  
  
  
  
/* Include file */
  val parseDeclarations: Parser[List[Declaration]] = parseDeclaration*
  
/* Orc program */
  val parseProgram: Parser[Expression] = parseExpression

  
  
  
  
/* Helper functions */

  def CommaSeparated[T](P: => Parser[T]): Parser[List[T]] = repsep(P, ",")

  def CommaSeparated1[T](P: => Parser[T]): Parser[List[T]] = rep1sep(P, ",")

  def TupleOf[T](P: => Parser[T]): Parser[List[T]] = "(" ~> CommaSeparated(P) <~ ")"

  def ListOf[T](P: => Parser[T]): Parser[List[T]] = "[" ~> CommaSeparated(P) <~ "]"

  def RecordOf[T](separator: String, P: => Parser[T]): Parser[List[(String,T)]] = {
    def parseEntry: Parser[(String,T)] = {
      (ident <~ separator) ~ P ^^ { case x ~ e => (x,e) }
    }
    "{." ~> CommaSeparated(parseEntry) <~ ".}"
  }  
    
  val parseConstantListTuple: Parser[Expression] = (
      "-" ~> numericLit -> { s => Constant(-BigInt(s)) }
    | "-" ~> floatLit -> { s => Constant(-BigDecimal(s)) }
    | parseValue -> Constant
    | "(" ~> parseConstantListTuple <~ ")"
    | ListOf(parseConstantListTuple) -> ListExpr
    | TupleOf(parseConstantListTuple) -> TupleExpr
    )
    
  val parseGuard = "if" ~> "(" ~> parseExpression <~ ")"
  
  val parseReturnType = "::" ~> parseType  

  
  
}
