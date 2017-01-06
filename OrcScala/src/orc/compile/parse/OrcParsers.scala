//
// OrcParsers.scala -- Scala objects Orc...Parser, and class OrcParsers
// Project OrcScala
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.parse

import java.io.IOException

import scala.{ BigDecimal, BigInt }
import scala.language.{ implicitConversions, postfixOps }
import scala.util.parsing.combinator.syntactical.StandardTokenParsers

import orc.OrcCompilerRequires
import orc.ast.ext._
import orc.compile.CompilerOptions
import orc.error.OrcException
import orc.error.compiletime.IncludeFileException
import orc.util.SynchronousThreadExec

/** Mix-in for result types from Orc parsers
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
  type Error = OrcParsers#Error
  type Failure = OrcParsers#Failure
}

/** An Orc parser that analyzes a given string as an Orc literal value.
  * The result of applying this function is an Expression Extended AST,
  * in particular a Constant, ListValue, TupleValue, or RecordValue.
  *
  * @see orc.ast.ext.Expression
  * @see orc.ast.ext.Constant
  * @see orc.ast.ext.ListValue
  * @see orc.ast.ext.TupleValue
  * @author jthywiss
  */
object OrcLiteralParser extends (String => OrcParsers#ParseResult[Expression]) with OrcParserResultTypes[Expression] {
  def apply(s: String): ParseResult = {
    //FIXME: Remove this SynchronousThreadExec whe Scala Issue SI-4929 is fixed
    SynchronousThreadExec("Orc Parser Thread", {
      val parsers = new OrcParsers(null, null, null)
      val tokens = new parsers.lexical.Scanner(new OrcStringInputContext(s).reader)
      parsers.phrase(parsers.parserForReadSite)(tokens)
    })
  }
}

/** An Orc parser that analyzes a given input as an Orc program.
  * The result of applying this function is an Expression Extended AST
  * representing the whole program and included files.
  *
  * @author jthywiss
  */
object OrcProgramParser extends ((OrcInputContext, CompilerOptions, OrcCompilerRequires) => OrcParsers#ParseResult[Expression]) with OrcParserResultTypes[Expression] {
  def apply(ic: OrcInputContext, co: CompilerOptions, envServices: OrcCompilerRequires): ParseResult = {
    //FIXME: Remove this SynchronousThreadExec whe Scala Issue SI-4929 is fixed
    SynchronousThreadExec("Orc Parser Thread: " + ic.descr, {
      val parsers = new OrcParsers(ic, co, envServices)
      val tokens = new parsers.lexical.Scanner(ic.reader)
      parsers.phrase(parsers.parseProgram)(tokens)
    })
  }
}

/** An Orc parser that analyzes a given input as an Orc include file.
  * The result of applying this function is an Include Extended AST
  * representing the include and sub-included files.
  *
  * @author jthywiss
  */
object OrcIncludeParser extends ((OrcInputContext, CompilerOptions, OrcCompilerRequires) => OrcParsers#ParseResult[Include]) with OrcParserResultTypes[Include] {
  def apply(ic: OrcInputContext, co: CompilerOptions, envServices: OrcCompilerRequires): ParseResult = {
    //FIXME: Remove this SynchronousThreadExec when Scala Issue SI-4929 is fixed
    SynchronousThreadExec("Orc Parser Thread: " + ic.descr, {
      val newParsers = new OrcParsers(ic, co, envServices)
      val parseInclude = newParsers.markLocation(newParsers.parseDeclarations ^^ { Include(ic.descr, _) })
      val tokens = new newParsers.lexical.Scanner(ic.reader)
      newParsers.phrase(parseInclude)(tokens)
    })
  }
}

/** This is a container for the various Parsers that embody the Orc grammar
  * productions, and some parsing helper methods.
  * <p>
  * A fresh OrcParsers instance is needed for every parse.
  * (Limitation of scala.util.parsing.combinator.Parsers -- current parsing state kept in fields.)
  *
  * @author dkitchin, amshali, srosario, jthywiss
  */
class OrcParsers(inputContext: OrcInputContext, co: CompilerOptions, envServices: OrcCompilerRequires)
  extends StandardTokenParsers
  with CustomParserCombinators {
  import lexical.{ FloatingPointLit, Keyword }

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
   ?               postfix     dereference
   .               postfix     field projection
   ()              postfix     application
  ----------------------------------------------------
   ~               prefix      boolean not
   -               prefix      unary minus
  ----------------------------------------------------
   **              left        exponent
  ----------------------------------------------------
   *               left        multiplication
   /               left        division
   %               left        modulus
  ----------------------------------------------------
   +               left        addition
   -               left        subtraction
  ----------------------------------------------------
   :               right       cons
  ----------------------------------------------------
   =               none        equal
   /=              none        not equal
   :>              none        greater
   >               none        greater
   >=              none        greater or equal
   <:              none        less
   <               none        less
   <=              none        less or equal
  ----------------------------------------------------
   ||              both        boolean or
   &&              both        boolean and
  ----------------------------------------------------
   :=              none        ref assignment
  ----------------------------------------------------
   >>              right       sequence
  ----------------------------------------------------
   |               both        parallel
  ----------------------------------------------------
   <<              left        where
  ----------------------------------------------------
   ;               both        semicolon
  ----------------------------------------------------
   ::              left        type information
   :!:             left        type override
   lambda          right       anonymous function
   if then else    prefix      conditional
   def
   import
   type
   include         prefix      declaration
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

  ////////
  // Literals
  ////////

  val floatLit: Parser[String] =
    elem("number", _.isInstanceOf[FloatingPointLit]) ^^ (_.chars)

  val parseValue: Parser[AnyRef] = (
    "true" ^^^ java.lang.Boolean.TRUE
    | "false" ^^^ java.lang.Boolean.FALSE
    | "signal" ^^^ orc.values.Signal
    | stringLit
    | numericLit ^^ { BigInt(_) }
    | floatLit ^^ { BigDecimal(_) }
    | "null" ^^^ null)

  val parserForReadSite: Parser[Expression] = (
    "-" ~> numericLit -> { s => Constant(-BigInt(s)) }
    | "-" ~> floatLit -> { s => Constant(-BigDecimal(s)) }
    | parseValue -> Constant
    | "(" ~> parserForReadSite <~ ")"
    | ListOf(parserForReadSite) -> ListExpr
    | TupleOf(parserForReadSite) -> TupleExpr
    | RecordOf("=", parserForReadSite) -> RecordExpr)

  val parseSiteLocation = stringLit

  ////////
  // Expressions
  ////////

  val parseBaseExpressionTail: Parser[Option[List[Expression]]] = (
    "," ~> CommaSeparated1(parseExpression) <~ ")" ^^ { Some(_) }
    | ")" ^^^ None)

  val parseBaseExpression = (
    parseValue -> Constant
    | ident -> Variable
    | "_" -> Placeholder
    | "stop" -> Stop
    | "this" -> Variable("this")
    | "super" ~> "." ~> ident -> { f => Call(Variable("super"), List(FieldAccess(f))) }
    | ListOf(parseExpression) -> ListExpr
    | RecordOf("=", parseExpression) -> RecordExpr
    | "new" ~> parseNewClassExpression -> New
    | ("(" ~> parseExpression ~ parseBaseExpressionTail) -?->
    { (e: Expression, es: List[Expression]) => TupleExpr(e :: es) }
    | ("{|" ~> parseExpression <~ "|}") -> Trim
    | ("{" ~> parseExpression <~ "}") -> Section
    | "lambda" ~> (ListOf(parseTypeVariable)?)
        ~ (TupleOf(parsePattern))
        ~ (parseReturnType?)
        //~ (parseGuard?) TODO: guards on lambdas are not supported any more
        ~ ("=" ~> parseExpression)
        -> Lambda
    | failExpecting("expression"))

  val parseArgumentGroup: Parser[ArgumentGroup] = (
    "." ~> ident -> FieldAccess
    | "?" -> Dereference
    | (("[" ~> CommaSeparated(parseType) <~ "]")?) ~ ("(" ~> CommaSeparated(parseExpression) <~ ")") -> Args)

  val parseCallExpression: Parser[Expression] = (
    parseBaseExpression ~ ((parseArgumentGroup+)?) -?-> Call)

  val parseUnaryExpr: Parser[Expression] = (
    // First see if it's a unary minus for a numeric literal
    "-" ~> numericLit -> { s => Constant(-BigInt(s)) }
    | "-" ~> floatLit -> { s => Constant(-BigDecimal(s)) }
    | ("-" | "~") ~ parseCallExpression -> PrefixOperator
    | parseCallExpression)

  val parseExpnExpr = parseUnaryExpr rightAssociativeInfix List("**")
  val parseMultExpr = parseExpnExpr leftAssociativeInfix List("*", "/", "%")
  val parseAdditionalExpr = parseMultExpr leftAssociativeInfix List("-", "+")
  val parseConsExpr = parseAdditionalExpr rightAssociativeInfix List(":")
  val parseRelationalExpr = parseConsExpr nonAssociativeInfix List("<:", ":>", "<=", ">=", "=", "/=")
  val parseLogicalExpr = parseRelationalExpr fullyAssociativeInfix List("||", "&&")
  val parseInfixOpExpression = parseLogicalExpr nonAssociativeInfix List(":=")

  val parseSequentialCombinator = ">" ~> (parsePattern?) <~ ">"

  val parseSequentialExpression =
    parseInfixOpExpression rightInterleave parseSequentialCombinator apply Sequential

  val parseParallelExpression =
    rep1sep(parseSequentialExpression, "|") -> { _ reduceLeft Parallel }

  val parseOtherwiseExpression =
    rep1sep(parseParallelExpression, ";") -> { _ reduceLeft Otherwise }

  val parseAscription = (
    ("::" ~ parseType)
    | (":!:" ~ parseType)
    | failUnexpectedIn("expression"))

  val parseReturnType = "::" ~> parseType

  val parseGuard = "if" ~> "(" ~> parseExpression <~ ")"

  def parseExpression: Parser[Expression] = (
    parseOtherwiseExpression ~ (parseAscription?) -?->
    {
      (_, _) match {
        case (e, "::" ~ t) => TypeAscription(e, t)
        case (e, ":!:" ~ t) => TypeAssertion(e, t)
        case _ => throw new AssertionError("Internal parser failure (Failed ascription match)")
      }
    }
    | parseDeclaration ~ (parseExpression | failExpecting("after declaration, expression")) -> Declare
    | ("if" ~> parseExpression)
    ~ ("then" ~> parseExpression)
    ~ ("else" ~> parseExpression)
    -> Conditional
    | failExpecting("expression"))

  ////////
  // Patterns
  ////////

  val parseBasePatternTail: Parser[Option[List[Pattern]]] = (
    "," ~> CommaSeparated(parsePattern) <~ ")" ^^ { Some(_) }
    | ")" ^^^ None)

  val parseBasePattern = (
    "-" ~> numericLit -> { s => ConstantPattern(-BigInt(s)) }
    | "-" ~> floatLit -> { s => ConstantPattern(-BigDecimal(s)) }
    | parseValue -> ConstantPattern
    | "_" -> Wildcard
    | ident ~ TupleOf(parsePattern) -> CallPattern
    | ident -> VariablePattern
    | ("(" ~> parsePattern ~ parseBasePatternTail) -?->
    { (p: Pattern, ps: List[Pattern]) => TuplePattern(p :: ps) }
    | ListOf(parsePattern) -> ListPattern
    | RecordOf("=", parsePattern) -> RecordPattern
    | failExpecting("pattern"))

  val parseConsPattern = rep1sep(parseBasePattern, ":") -> (_ reduceRight ConsPattern)

  val parseAsPattern = (
    parseConsPattern ~ (("as" ~> ident)?) -?-> AsPattern)

  val parseTypedPattern = (
    parseAsPattern ~ (("::" ~> parseType)?) -?-> TypedPattern)

  val parsePattern: Parser[Pattern] = parseTypedPattern

  ////////
  // Types
  ////////

  val parseTypeVariable: Parser[String] = ident

  val parseTypeTail: Parser[Option[List[Type]]] = (
    "," ~> CommaSeparated1(parseType) <~ ")" ^^ { Some(_) }
    | ")" ^^^ None)

  val parseType: Parser[Type] = (
    parseTypeVariable ~ (ListOf(parseType)?) ->
    {
      (_, _) match {
        case (id, None) => TypeVariable(id)
        case (id, Some(ts)) => TypeApplication(id, ts)
      }
    }
    | ("(" ~> parseType ~ parseTypeTail) -?->
    { (t: Type, ts: List[Type]) => TupleType(t :: ts) }
    | TupleOf(parseType) -> TupleType
    | RecordOf("::", parseType) -> RecordType
    | "lambda" ~> ((ListOf(parseTypeVariable)?) ^^ { _.getOrElse(Nil) }) ~ (TupleOf(parseType)) ~ parseReturnType -> LambdaType
    | failExpecting("type"))

  val parseConstructor: Parser[Constructor] = (
    ident ~ TupleOf(parseType ^^ (Some(_))) -> Constructor
    | ident ~ TupleOf("_" ^^^ None) -> Constructor)

  ////////
  // Declarations
  ////////

  val parseDefSigCoreNamed = (
    ident ~ (ListOf(parseTypeVariable)?) ~ (TupleOf(parsePattern)) ~ (parseReturnType?))

  val parseDefCore = (
    parseDefSigCoreNamed ~ (parseGuard?) ~ ("=" ~> parseExpression))

  val parseDefSigCore = (
    ident ~ (ListOf(parseTypeVariable)?) ~ (TupleOf(parseType)) ~ parseReturnType)

  val parseDefDeclaration: Parser[CallableDeclaration] = (
    parseDefCore -> Def

    | parseDefSigCore -> DefSig)

  val parseSiteDeclaration: Parser[CallableDeclaration] = (
    parseDefCore -> Site

    | parseDefSigCore -> SiteSig)

  val parseClassBody: Parser[ClassLiteral] = (
    ("{" ~> (ident <~ "#").? ~ parseDeclaration.* <~ "}") -> ClassLiteral)

  val parseClassPrimitiveExpression: Parser[ClassExpression] = (
    parseClassBody
    | ident -> ClassVariable
    | "(" ~> parseClassExpression <~ ")")
  val parseClassExpression: Parser[ClassExpression] = (
    parseClassPrimitiveExpression ~ ("with" ~> parseClassExpression) -> ClassMixin
    | parseClassPrimitiveExpression)

  val parseNewClassExpression = (
    (ident -> ClassVariable
      | "(" ~> parseClassExpression <~ ")") ~ parseClassBody -> ClassSubclassLiteral
      | parseClassExpression)

  val parseClassConstructor: Parser[ClassConstructor] = (
    ident ~ (ListOf(parseTypeVariable)?) -> ClassConstructor.None
    | "def" ~> parseDefSigCoreNamed -> ClassConstructor.Def
    | "site" ~> parseDefSigCoreNamed -> ClassConstructor.Site)

  val parseClassDeclaration = (
    (parseClassConstructor ~ ("extends" ~> parseClassExpression).? ~ parseClassBody) -> ClassDeclaration)

  val parseDeclaration: Parser[Declaration] = (
    (

      ("val" ~> parsePattern) ~ ("=" ~> parseExpression) -> Val

      | ("val" ~> ident ~ ("::" ~> parseType).?) -> ValSig

      | "def" ~> parseDefDeclaration

      | "site" ~> parseSiteDeclaration

      | "class" ~> parseClassDeclaration

      | "import" ~> Keyword("site") ~> ident ~ ("=" ~> parseSiteLocation) -> SiteImport

      | "import" ~> Keyword("class") ~> ident ~ ("=" ~> parseSiteLocation) -> ClassImport

      | !@(("include" ~> stringLit).into(performInclude))

      | "import" ~> "type" ~> ident ~ ("=" ~> parseSiteLocation) -> TypeImport

      | "type" ~> parseTypeVariable ~ ((ListOf(parseTypeVariable))?) ~ ("=" ~> rep1sep(parseConstructor, "|"))
      -> ((x, ys, t) => Datatype(x, ys getOrElse Nil, t))

      | "type" ~> parseTypeVariable ~ ((ListOf(parseTypeVariable))?) ~ ("=" ~> parseType)
      -> ((x, ys, t) => TypeAlias(x, ys getOrElse Nil, t))) <~ ("#"?) /* Optional declaration terminator # */

      | failExpecting("declaration (val, def, type, etc.)"))

  /** Sets the position of any OrcException thrown by this
    * parser to the starting position of this parse.
    */
  def !@[U](x: Parser[U]): Parser[U] =
    Parser { in =>
      val startPos = ToTextRange(in.pos)
      try {
        x(in)
      } catch {
        case e: OrcException => {
          val endPos = ToTextRange(in.pos)
          val parsedRange = new OrcSourceRange((startPos.start, endPos.end))
          throw e.setPosition(parsedRange)
        }
      }
    }

  def performInclude(includeName: String): Parser[Include] = {
    val newInputContext = try {
      envServices.openInclude(includeName, inputContext, co.options)
    } catch {
      case e: IOException => throw new IncludeFileException(includeName, e)
    }
    co.compileLogger.beginDependency(newInputContext);
    try {
      OrcIncludeParser(newInputContext, co, envServices) match {
        case r: OrcIncludeParser.SuccessT[_] => success(r.get)
        case n: OrcIncludeParser.NoSuccess => Parser { in => Error(n.msg, new Input { def first = null; def rest = this; def pos = n.next.pos; def atEnd = true }) }
      }
    } finally {
      co.compileLogger.endDependency(newInputContext);
    }
  }

  ////////
  // Include file
  ////////

  val parseDeclarations: Parser[List[Declaration]] = parseDeclaration*

  ////////
  // Orc program
  ////////

  val parseProgram: Parser[Expression] = parseExpression

  ////////
  // Helper functions
  ////////

  def CommaSeparated[T](P: => Parser[T]): Parser[List[T]] = repsep(P, ",")

  def CommaSeparated1[T](P: => Parser[T]): Parser[List[T]] = rep1sep(P, ",")

  def TupleOf[T](P: => Parser[T]): Parser[List[T]] = "(" ~> CommaSeparated(P) <~ ")"

  def ListOf[T](P: => Parser[T]): Parser[List[T]] = "[" ~> CommaSeparated(P) <~ "]"

  def RecordOf[T](separator: String, P: => Parser[T]): Parser[List[(String, T)]] = {
    def parseEntry: Parser[(String, T)] = {
      (ident <~ separator) ~ P ^^ { case x ~ e => (x, e) }
    }
    "{." ~> CommaSeparated(parseEntry) <~ ".}"
  }

  /* Override scala.util.parsing.combinator.Parsers.accept to clean up
   * bad interaction between lexical ErrorTokens containing an error
   * message and the higher-level parser error message */
  override implicit def accept(e: Elem): Parser[Elem] = acceptIf(_ == e)(_ match {
    case et: lexical.ErrorToken => et.chars.stripPrefix("*** error: ")
    case in => "" + e + " expected, but " + in + " found"
  })

  def failExpecting(symbolName: String) = Parser { in =>
    in.first match {
      case et: lexical.ErrorToken => Failure(et.chars.stripPrefix("*** error: "), in)
      case t => Failure(symbolName + " expected, but " + t + " found", in)
    }
  }

  def failUnexpectedIn(symbolName: String) = Parser { in =>
    in.first match {
      case et: lexical.ErrorToken => Failure(et.chars.stripPrefix("*** error: "), in)
      case t => Failure("while parsing " + symbolName + ", found unexpected " + t, in)
    }
  }
}
