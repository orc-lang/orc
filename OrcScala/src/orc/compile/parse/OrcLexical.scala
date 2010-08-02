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
 * Lexical scanner (tokenizer) for Orc.  This extends and overrides
 * <code>scala.util.parsing.combinator.lexical.StdLexical</code>
 * with Orc's lexical definitions.
 *
 * @author jthywiss
 */

class OrcLexical() extends StdLexical() {

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
    | integerLit                                        ^^ { case i => NumericLit(i) }
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

  def plusMinusIntegerLit =
      ( elem('+') ~ integerLit ^^ { case a ~ b => a+b }
      | elem('-') ~ integerLit ^^ { case a ~ b => a+b }
      | integerLit )

  def floatLit =
    ( integerLit ~ '.' ~ integerLit ~ (elem('e') |  elem('E')) ~ plusMinusIntegerLit ^^ { case a ~ b ~ c ~ d ~ e => a+b+c+d+e }
    | integerLit                    ~ (elem('e') |  elem('E')) ~ plusMinusIntegerLit ^^ { case a ~ b ~ c => a+b+c }
    | integerLit ~ '.' ~ integerLit                                                  ^^ { case a ~ b ~ c => a+b+c }
    )

  override def whitespaceChar = elem("space char", ch => ch == ' ' || ch == '\t' || ch == '\r' || ch == '\f')

  override def whitespace: Parser[Any] = rep(
      whitespaceChar
    | multilinecomment
    | '-' ~ '-' ~ rep( chrExcept(EofCh, '\n') )
    | '{' ~ '-' ~ failure("unclosed comment")
    )

  def multilinecomment: Parser[Any] =
      '{' ~ '-' ~ rep(endcomment) ~ '-' ~ '}' ^^ { case _ => ' ' }

  def endcomment: Parser[Any] = (
     multilinecomment // Allow inlined comments.
   | not(guard('-' ~ '}')) ~ chrExcept(EofCh)
   | '-' ~ guard('-')
   | chrExcept(EofCh,'-','}')
  )

  protected lazy val sortedOperParsers = {
    def parseOper(s: String): Parser[Token] = accept(s.toList) ^^ { x => Keyword(s) }
    val o = new Array[String](operators.size)
    operators.copyToArray(o, 0)
    scala.util.Sorting.quickSort(o)
    o.toList map parseOper
  }

  protected def oper: Parser[Token] = {
    sortedOperParsers.foldRight(failure("no matching operator"): Parser[Token])((x, y) => y | x)
  }


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
      "(", ")", "[", "]", "{.", ".}", ",",
       "|", ";",
      "::", ":!:"
    ) ::: operators)

}
