//
// OrcLexical.scala -- Scala class/trait/object OrcLexical
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
import scala.util.parsing.input.CharArrayReader.EofCh
import scala.collection.mutable.HashSet

/**
 * 
 *
 * @author jthywiss
 */

class OrcLexical() extends StdLexical() {
  
  case class FloatingPointLit(chars: String) extends Token {
    override def toString = chars
  }

  override def token: Parser[Token] = 
    ( identChar ~ rep( identChar | digit )              ^^ { case first ~ rest => processIdent(first :: rest mkString "") }
    | '(' ~ delim ~ ')'                                 ^^ { case '(' ~ d ~ ')' => Identifier(d.chars) }
    | '(' ~ '0' ~ '-' ~ ')'                             ^^ { _ => Identifier("0-") }
    | delim
    | '\"' ~ rep( chrExcept('\"', '\n', EofCh) ) ~ '\"' ^^ { case '\"' ~ chars ~ '\"' => StringLit(chars mkString "") }
    | '\"' ~> failure("unclosed string literal")        
    | floatLit                                          ^^ { case f => FloatingPointLit(f) }
    | signedIntegerLit                                  ^^ { case i => NumericLit(i) }
    | EofCh                                             ^^^ EOF
    | failure("illegal character")
    )
  
  // legal identifier chars other than digits
  override def identChar = letter | elem('_') | elem('\'')

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
  
  override def whitespaceChar = elem("space char", ch => ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f')

  override def whitespace: Parser[Any] = rep(
      whitespaceChar
    | '{' ~ '-' ~ comment
    | '-' ~ '-' ~ rep( chrExcept(EofCh, '\n') )
    | '{' ~ '-' ~ failure("unclosed comment")
    )

  override protected def comment: Parser[Any] = (
      '-' ~ '}'  ^^ { case _ => ' '  }
    | chrExcept(EofCh) ~ comment
    )

  /** The set of reserved identifiers: these will be returned as `Keyword's */
  override val reserved = new HashSet[String] ++ List(
      "true", "false", "signal", "stop", "null",
      "lambda", "if", "then", "else", "as",
      "val", "def", "type", "site", "class", "include",
      "Top", "Bot"
    )

  /** The set of delimiters (ordering does not matter) */
  override val delimiters = new HashSet[String] ++ List(
      "+", "-", "*", "/", "%", "**",
      "&&", "||", "~",
      "<", ">", "|", ";",
      "(", ")", "[", "]", "{", "}", ",",
      "=", "<:", ":>", "<=", ">=", "/=",
      ":", "_", "++",
      ".", "?", ":=",
      "::", ":!:"
    )

}
