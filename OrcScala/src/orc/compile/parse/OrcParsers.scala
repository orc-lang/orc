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

import orc.AST
import orc.OrcOptions
import orc.OrcCompilerRequires
import orc.compile.ext._


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
 * @see orc.compile.ext.Expression
 * @see orc.compile.ext.Constant
 * @see orc.compile.ext.ListValue
 * @see orc.compile.ext.TupleValue
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
object OrcProgramParser extends ((OrcInputContext, OrcOptions, OrcCompilerRequires) => OrcParsers#ParseResult[Expression]) with OrcParserResultTypes[Expression] {
  def apply(ic: OrcInputContext, options: OrcOptions, envServices: OrcCompilerRequires): ParseResult = {
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
object OrcIncludeParser extends ((OrcInputContext, OrcOptions, OrcCompilerRequires) => OrcParsers#ParseResult[Include]) with OrcParserResultTypes[Include] {
  def apply(ic: OrcInputContext, options: OrcOptions, envServices: OrcCompilerRequires): ParseResult = {
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
class OrcParsers(inputContext: OrcInputContext, options: OrcOptions, envServices: OrcCompilerRequires) extends StandardTokenParsers {
  import lexical.{Keyword, FloatingPointLit}

  override val lexical = new OrcLexical()

  ////////
  // Grammar productions (in bottom-up order)
  ////////

  def parseClassname: Parser[String] = (
        stringLit
        // For backwards compatibility, allow quotes to be omitted, if class name had only Orc-legal identifier characters
      | rep1sep(ident, ".") ^^ { _.mkString(".") }
  )

  def parseStrictClassname: Parser[String] = (
        stringLit
        // For backwards compatibility, allow quotes to be omitted, if class name had only Orc-legal identifier characters
      | ident ~ "." ~ rep1sep(ident, ".") ^^ { case x ~ "." ~ xs => x + "." + xs.mkString(".") }
  )

  def floatLit: Parser[String] =
    elem("number", _.isInstanceOf[FloatingPointLit]) ^^ (_.chars)

  def parseValue: Parser[AnyRef] = (
        "true" ^^^ java.lang.Boolean.TRUE
      | "false" ^^^ java.lang.Boolean.FALSE
      | "signal" ^^^ orc.values.Signal
      | stringLit
      | numericLit ^^ { BigInt(_) }
      | floatLit ^^ { BigDecimal(_) }
      | "null" ^^^ null
  )

  def parseTypeVariable: Parser[String] = ident

  def parseRecordEntry: Parser[(String, Expression)] =
    (ident <~ "=") ~ parseExpression ^^ { case x ~ e => (x,e) }

  def parseBaseExpressionTail: Parser[Option[List[Expression]]] = (
        ")" ^^^ None
      | "," ~> CommaSeparated1(parseExpression) <~ ")" ^^ {Some(_)}
  )

  def parseBaseExpression = (
        parseValue -> Constant
      | ident -> Variable
      | "stop" -> Stop
      | ("[" ~> CommaSeparated(parseExpression) <~ "]") -> ListExpr
      | ("{." ~> CommaSeparated(parseRecordEntry) <~ ".}") -> RecordExpr
      | ("(" ~> parseExpression ~ parseBaseExpressionTail) -?->
            { (e: Expression, es: List[Expression]) => TupleExpr(e::es) }
  )

  def parseArgumentGroup: Parser[ArgumentGroup] = (
      (("[" ~> CommaSeparated(parseType) <~ "]")?) ~ ("(" ~> CommaSeparated(parseExpression) <~ ")") -> Args
      | "." ~> ident -> FieldAccess
      | "?" -> Dereference
  )

  def parseCallExpression: Parser[Expression] = (
        parseBaseExpression ~ ((parseArgumentGroup+)?) -?-> Call
  )

  def parseConditionalExpression: Parser[Expression] = (
        ("if" ~> parseOtherwiseExpression)
        ~ ("then" ~> parseOtherwiseExpression)
        ~ ("else" ~> parseOtherwiseExpression)
        -> Conditional
    |
      parseCallExpression
  )

  def parseUnaryExpr = (
    // First see if it's a unary minus for a numeric literal
      "-" ~> numericLit -> { s => Constant(-BigInt(s)) }
    | "-" ~> floatLit -> { s => Constant(-BigDecimal(s)) }
    | ("-" | "~") ~ parseConditionalExpression -> PrefixOperator
    | parseConditionalExpression
    )

  //FIXME: All these uses of ^^ are discarding position information!

  def parseExpnExpr = chainl1(parseUnaryExpr, ("**") ^^
    { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })

  def parseMultExpr = chainl1(parseExpnExpr, ("*" | "/" | "%") ^^
    { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })

  def parseAdditionalExpr: Parser[Expression] = (
     chainl1(parseMultExpr, ("-" | "+") ^^
        { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })
        /* Disallow newline breaks for binary subtract,
         * to resolve ambiguity with unary minus.*/
  )

  def parseConsExpr: Parser[Expression] = (
     chainr1(parseAdditionalExpr, ":" ^^
        { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })
  )

  def parseRelationalExpr = chainl1(parseConsExpr, ("<:" | ":>" | "<=" | ">=" | "=" | "/=") ^^
        { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })

  def parseLogicalExpr = chainl1(parseRelationalExpr, ("||" | "&&") ^^
   { op =>(left:Expression,right:Expression) => InfixOperator(left, op, right)})

  def parseInfixOpExpression: Parser[Expression] = chainl1(parseLogicalExpr, ":=" ^^
    { op => (left:Expression,right:Expression) => InfixOperator(left, op, right) })

  def parseSequentialCombinator = ">" ~> (parsePattern?) <~ ">"

  def parsePruningCombinator = "<" ~> (parsePattern?) <~ "<"

  def parseSequentialExpression =
    parseInfixOpExpression interleaveRight parseSequentialCombinator apply Sequential

  def parseParallelExpression =
    rep1sep(parseSequentialExpression, "|") -> (_ reduceLeft Parallel)

  def parsePruningExpression =
    parseParallelExpression interleaveLeft parsePruningCombinator apply Pruning

  def parseOtherwiseExpression =
    rep1sep(parsePruningExpression, ";") -> (_ reduceLeft Otherwise)

/*---------------------------------------------------------
  Expressions
  
  For reference, here are Orc's operators sorted
  and grouped in order of increasing precedence.
  
  Symbol   Assoc       Name
  ----------------------------------------------------
   ::      right       type annotation
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

  def parseAscription = (
        ("::" ~ parseType)
      | (":!:" ~ parseType)
  )

  def parseExpression: Parser[Expression] = (
      "lambda" ~> (ListOf(parseType)?)
      ~ (TupleOf(parsePattern)+)
      ~ (("::" ~> parseType)?)
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
//    | failure("Goal expression expected.")
  )

  def parseBasePatternTail: Parser[Option[List[Pattern]]] = (
        ")" ^^^ None
      | "," ~> CommaSeparated(parsePattern) <~ ")" ^^ {Some(_)}
  )

  def parseBasePattern = (
      parseValue -> ConstantPattern
      | "_" -> Wildcard
      | ident ~ TupleOf(parsePattern) -> CallPattern
      | ident -> VariablePattern
      | ("(" ~> parsePattern ~ parseBasePatternTail) -?->
          { (p: Pattern, ps: List[Pattern]) => TuplePattern(p::ps) }
      | ListOf(parsePattern) -> ListPattern
      | ("=" ~> ident) -> EqPattern
  )

  def parseConsPattern = rep1sep(parseBasePattern, ":") -> (_ reduceRight ConsPattern)

  def parseAsPattern = (
      parseConsPattern ~ (("as" ~> ident)?) -?-> AsPattern
  )

  def parseTypedPattern = (
      parseAsPattern ~ (("::" ~> parseType)?) -?-> TypedPattern
  )

  def parsePattern: Parser[Pattern] = parseTypedPattern

  def parseRecordTypeEntry: Parser[(String, Type)] =
    (ident <~ "::") ~ parseType ^^ { case x ~ t => (x,t) }

  def parseType: Parser[Type] = (
        "Top" -> Top
      | "Bot" -> Bot
      | parseTypeVariable ~ (ListOf(parseType)?) ->
        { (_,_) match
          {
            case (id, None) => TypeVariable(id)
            case (id, Some(ts)) => TypeApplication(id, ts)
          }
        }
      | TupleOf(parseType) -> TupleType
      | ("{." ~> CommaSeparated(parseRecordTypeEntry) <~ ".}") -> RecordType
      | "lambda" ~> ((ListOf(parseTypeVariable)?) ^^ {_.getOrElse(Nil)}) ~ (TupleOf(parseType)+) ~ parseReturnType -> LambdaType
  )

  def parseConstructor: Parser[Constructor] = (
      ident ~ TupleOf(parseType ^^ (Some(_))) -> Constructor
      | ident ~ TupleOf("_" ^^^ None) -> Constructor
  )

  def parseReturnType = "::" ~> parseType

  def parseDefDeclaration: Parser[DefDeclaration] = (
        ident ~ (TupleOf(parsePattern)+) ~ (parseReturnType?) ~ ("=" ~> parseExpression)
      -> Def

      | ("capsule" ~> ident) ~ (TupleOf(parsePattern)+) ~ (parseReturnType?) ~ ("=" ~> parseExpression)
      -> DefCapsule

      | ident ~ (ListOf(parseTypeVariable)?) ~ (TupleOf(parseType)+) ~ parseReturnType
      -> { (id, tvs, ts, rt) => DefSig(id, tvs getOrElse Nil, ts, rt) }
  )

  def parseDeclaration: Parser[Declaration] = (
      ("val" ~> parsePattern) ~ ("=" ~> parseExpression)
      -> Val

      | "def" ~> parseDefDeclaration

      | "site" ~> ident ~ ("=" ~> parseClassname)
      -> SiteImport

      | "class" ~> ident ~ ("=" ~> parseClassname)
      -> ClassImport

      | ("include" ~> stringLit).into(performInclude)

      | "type" ~> parseTypeVariable ~ ("=" ~> parseStrictClassname)
      -> TypeImport

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

  //def parseDeclarations: Parser[List[Declaration]] = (lexical.NewLine*) ~> (parseDeclaration <~ (lexical.NewLine*))*

  def parseDeclarations: Parser[List[Declaration]] = parseDeclaration*

  def parseProgram: Parser[Expression] = parseExpression

  ////////
  // Helper combinators for ( ... ) and [ ... ] forms
  ////////

  def CommaSeparated[T](P: => Parser[T]): Parser[List[T]] = repsep(P, ",")

  def CommaSeparated1[T](P: => Parser[T]): Parser[List[T]] = rep1sep(P, ",")

  def TupleOf[T](P: => Parser[T]): Parser[List[T]] = "(" ~> CommaSeparated(P) <~ ")"

  def ListOf[T](P: => Parser[T]): Parser[List[T]] = "[" ~> CommaSeparated(P) <~ "]"

  def parseConstantListTuple: Parser[Expression] = (
      "-" ~> numericLit -> { s => Constant(-BigInt(s)) }
    | "-" ~> floatLit -> { s => Constant(-BigDecimal(s)) }
    | parseValue -> Constant
    | "(" ~> parseConstantListTuple <~ ")"
    | ListOf(parseConstantListTuple) -> ListExpr
    | TupleOf(parseConstantListTuple) -> TupleExpr
    )

  ////////
  // Preserve input source position
  ////////

  class LocatingParser[+A <: AST](p: => Parser[A]) extends Parser[A] {
    override def apply(i: Input) = {
      val position = i.pos
      val result: ParseResult[A] = p.apply(i)
      result map { _.pos = position }
      result
    }
  }

  def markLocation[A <: AST](p: => Parser[A]) = new LocatingParser(p)

  ////////
  // Re-write some error messages with explanatory detail
  ////////

  def enhanceErrorMsg(r: ParseResult[Expression]): ParseResult[Expression] = {
    r match {
      case Failure(msg, in) => {
        if (msg.equals("``('' expected but EOF found")) {
          Failure(msg+"\n"+
              "  This error usually means that the expression is incomplete.\n" +
              "The following cases can create this parser problem:\n" +
              "  1. The right hand side expression in a combinator is missing.\n" +
              "  2. The expression missing after a comma.\n" +
              "  3. The goal expression of the program is missing.\n"
              , in)
        } else if (msg.startsWith("`NewLine' expected but `(' found")) {
          Failure(msg+"\n"+
              "  This error usually means that there are unexpected new lines\n" +
              "right after the name of a function or site in a call. The `('\n" +
              "should come after the name of the function or site and cannot\n" +
              "be separated with new lines.\n"
              , in)
        } else if (msg.startsWith("`NewLine' expected but")) {
          val name = msg.substring(23, msg.lastIndexOf("found")-1)
          Failure(msg+"\n"+
              "  This error usually means that there is an unexpected expression\n" +
              "before the "+name+" or the "+name+" itself is unexpected at this point.\n"
              , in)
        }
        else if (msg.startsWith("``::'' expected but")) {
          Failure(msg+"\n"+
              "  This error usually means that the `=' is missing in front of\n" +
              "the funtion definition.\n" +
              "  In case you want to specify the return type of the function\n" +
              "use `::' along with the type name."
              , in)
        }
        else if (msg.startsWith("``('' expected but `)' found")) {
          Failure(msg+"\n"+
              "  This error usually means an illegal start for an expression with `)'.\n" +
              "Check for mismatched parentheses."
              , in)
        }
        else if (msg.startsWith("``('' expected but")) {
          val name = msg.substring(19, msg.lastIndexOf("found")-1)
          Failure(msg+"\n"+
              "  This error usually means an illegal start for an expression with "+name+".\n"
              , in)
        }
        else if (msg.startsWith("``<'' expected but")) {
          Failure(msg+"\n"+
              "  This error usually happens in the following occasions:\n" +
              "  1. You are intending to use the pruning combinator `<<' and\n" +
              "     you have typed `<'.\n" +
              "  2. You are intending to use `<' as less than operator in which\n" +
              "     case you should know that the less than operator in Orc is `<:'."
              , in)
        }
        else if (msg.startsWith("``>'' expected but")) {
          Failure(msg+"\n"+
              "  This error usually happens in the following occasions:\n" +
              "  1. You are intending to use the sequential combinator `>>' and\n" +
              "     you have typed `>'.\n" +
              "  2. You are intending to use `>' as greater than operator in which\n" +
              "     case you should know that the greater than operator in Orc is `:>'."
              , in)
        }
        else r
      }
      case _ => r
    }
  }

  ////////
  // Extended apply combinator ->
  ////////

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

  class Maps1Optional2[A <: AST,B](parser: Parser[A ~ Option[B]]) {

  def -?->(f: (A,B) => A): Parser[A] =
      markLocation(parser ^^ {
        case x ~ None => x
        case x ~ Some(y) => f(x,y)
      })
  }

  ////////
  // Interleaving combinator
  ///////

  class InterleavingParser[A <: AST](parser: Parser[A]) {
    def interleaveLeft[B](interparser: Parser[B]) =
      (f: (A,B,A) => A) => {
        def origami(b: B)(x:A, y:A): A = f(x,b,y)
        markLocation( chainl1(markLocation(parser), interparser ^^ origami) )
      }: Parser[A]
    def interleaveRight[B](interparser: Parser[B]) =
      (f: (A,B,A) => A) => {
        def origami(b: B)(x:A, y:A): A = f(x,b,y)
        markLocation( chainr1(markLocation(parser), interparser ^^ origami) )
      }: Parser[A]
  }

  ////////
  // Our own chainr1
  ////////

  def chainr1[T](p: => Parser[T], q: => Parser[(T, T) => T]): Parser[T] = {
      def myFold[T](list: List[((T,T)=>T) ~ T]): (T => T) = {
        list match {
          case List(f ~ a) => f(_,a)
          case f ~ a :: xs => f(_,myFold(xs)(a))
        }
      }
      p ~ rep(q ~ p) ^^ {case x ~ xs => if (xs.isEmpty) x else myFold(xs)(x)}
  }

  ////////
  // implicit conversions (views) for parsers
  ////////

  implicit def CreateMaps0Parser(s: String): Maps0 = new Maps0(s)
  implicit def CreateMaps1Parser[A](parser: Parser[A]): Maps1[A] = new Maps1(parser)
  implicit def CreateMaps2Parser[A,B](parser: Parser[A ~ B]): Maps2[A,B] =	new Maps2(parser)
  implicit def CreateMaps3Parser[A,B,C](parser: Parser[A ~ B ~ C]): Maps3[A,B,C] = new Maps3(parser)
  implicit def CreateMaps4Parser[A,B,C,D](parser: Parser[A ~ B ~ C ~ D]): Maps4[A,B,C,D] = new Maps4(parser)

  implicit def CreateMaps1Optional2Parser[A <: AST,B](parser: Parser[A ~ Option[B]]): Maps1Optional2[A,B] = new Maps1Optional2(parser)

  implicit def CreateInterleavingParser[A <: AST](parser: Parser[A]): InterleavingParser[A] = new InterleavingParser(parser)
}
