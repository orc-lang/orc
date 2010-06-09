//
// OrcLexical.scala -- Scala class OrcLexical
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 1, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.parse

import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.input.CharSequenceReader.EofCh
import scala.util.parsing.input.Reader
import scala.collection.mutable.HashSet

/**
 *
 *
 * @author jthywiss
 */

class OrcLexical() extends StdLexical() {

  class OrcScanner(in: Reader[Char]) extends Reader[Token] with NamedSubfileReader[Token] {
//    def this(in: String) = this(new CharArrayReader(in.toCharArray()))
    private val (tok, rest1, rest2) = whitespace(in) match {
      case Success(_, in1) => 
        token(in1) match {
          case Success(tok, in2) => (tok, in1, in2)
          case ns: NoSuccess => (errorToken(ns.msg), ns.next, skip(ns.next))
        }
      case ns: NoSuccess => (errorToken(ns.msg), ns.next, skip(ns.next))
    }
    private def skip(in: Reader[Char]) = if (in.atEnd) in else in.rest

    override def source: java.lang.CharSequence = in.source
    override def offset: Int = in.offset
    override def first = tok
    override def rest = new OrcScanner(rest2)
    override def pos = rest1.pos
    override def atEnd = in.atEnd || (whitespace(in) match { case Success(_, in1) => in1.atEnd case _ => false })

    val descr = in match {
      case r: NamedSubfileReader[_] => r.descr
      case _ => ""
    }
    def newSubReader(newFilename: String) = in match {
      case r: NamedSubfileReader[_] => r.newSubReader(newFilename)
      case _ => throw new orc.error.compiletime.ParsingException("Cannot process includes from this input source (type="+in.getClass().getName()+")", in.pos)
    }
  }

  case class FloatingPointLit(chars: String) extends Token {
    override def toString = chars
  }

  case object NewLine extends Token {
    def chars = "\n"
  }
  
  override def token: Parser[Token] =
    ( letter ~ rep( identChar | digit )                 ^^ { case first ~ rest => processIdent(first :: rest mkString "") }
    | '_'                                               ^^^ Keyword("_")
    | '(' ~ oper ~ ')'                                  ^^ { case '(' ~ o ~ ')' => Identifier(o.chars) }
    | '(' ~ '0' ~ '-' ~ ')'                             ^^^ Identifier("0-")
    | floatLit                                          ^^ { case f => FloatingPointLit(f) }
    | signedIntegerLit                                  ^^ { case i => NumericLit(i) }
    | '\"' ~ rep(stringLitChar) ~ '\"'                  ^^ { case '\"' ~ chars ~ '\"' => StringLit(chars mkString "") }
    | '\"' ~> failure("unclosed string literal")
    | delim   // Must be after other alternatives that a delim could be a prefix of
    | EofCh                                             ^^^ EOF
    | '\n'                                              ^^^ NewLine
    | '\r' ~ '\n'                                       ^^^ NewLine
    | '\r'                                              ^^^ NewLine
    | failure("illegal character")
    )  
  
  // legal identifier chars other than digits
  override def identChar = letter | elem('_') | elem('\'')
  
  def stringLitChar = 
    ( '\\' ~> chrExcept(EofCh)      ^^ { case 'f' => '\f'; case 'n' => '\n'; case 'r' => '\r'; case 't' => '\t'; case c => c }
    | chrExcept('\"', '\n', EofCh)
    )

  def nonZeroDigit = elem('1') | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
  def integerLit =
      ( elem('0')                 ^^ { _.toString() }
      | rep1(nonZeroDigit, digit) ^^ { _ mkString "" }
      )

  def signedIntegerLit =
      ( elem('-') ~ integerLit ^^ { case a ~ b => a+b }
      | integerLit )
  def plusMinusIntegerLit =
      ( elem('+') ~ integerLit ^^ { case a ~ b => a+b }
      | signedIntegerLit )

  def floatLit =
    ( signedIntegerLit ~ '.' ~ integerLit ~ (elem('e') |  elem('E')) ~ plusMinusIntegerLit ^^ { case a ~ b ~ c ~ d ~ e => a+b+c+d+e }
    | signedIntegerLit                    ~ (elem('e') |  elem('E')) ~ plusMinusIntegerLit ^^ { case a ~ b ~ c => a+b+c }
    | signedIntegerLit ~ '.' ~ integerLit                                                  ^^ { case a ~ b ~ c => a+b+c }
    )

  override def whitespaceChar = elem("space char", ch => ch == ' ' || ch == '\t' || ch == '\r' || ch == '\f')

  override def whitespace: Parser[Any] = rep(
      whitespaceChar
    | multilinecomment
    | '-' ~ '-' ~ rep( chrExcept(EofCh, '\n') )
    | '{' ~ '-' ~ failure("unclosed comment")
    )

  def multilinecomment: Parser[Any] =
      '{' ~ '-' ~ endcomment

  protected def oper: Parser[Token] = {
    def parseOper(s: String): Parser[Token] = accept(s.toList) ^^ { x => Keyword(s) }
    val o = new Array[String](operators.size)
    operators.copyToArray(o, 0)
    scala.util.Sorting.quickSort(o)
    (o.toList map parseOper).foldRight(failure("no matching operator"): Parser[Token])((x, y) => y | x)
  }

  def endcomment: Parser[Any] = (
    '-' ~ '}' ^^ { case _ => ' ' }
   | '{' ~ '-' ~ endcomment ~ endcomment // Allow inlined comments.
   | chrExcept(EofCh) ~ endcomment
  )
    
  /** The set of reserved identifiers: these will be returned as `Keyword's */
  override val reserved = new HashSet[String] ++ List(
      "true", "false", "signal", "stop", "null",
      "lambda", "if", "then", "else", "as", "_",
      "val", "def", "capsule", "type", "site", "class", "include",
      "Top", "Bot"
    )

  val operators = List(
      "+", "-", "*", "/", "%", "**",
      "&&", "||", "~",
      "<", ">",
      "=", "<:", ":>", "<=", ">=", "/=",
      ":", "++",
      ".", "?", ":="
    )

  /** The set of delimiters (ordering does not matter) */
  override val delimiters = new HashSet[String] ++ (List(
      "(", ")", "[", "]", "{", "}", ",",
       "|", ";",
      "::", ":!:"
    ) ::: operators)

}
