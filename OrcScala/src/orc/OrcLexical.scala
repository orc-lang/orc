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
package orc

import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.input.CharArrayReader.EofCh
import scala.collection.mutable.HashSet

/**
 * 
 *
 * @author jthywiss
 */

class OrcLexical() extends StdLexical() {

  // see `token' in `Scanners'
  override def token: Parser[Token] = 
    ( identChar ~ rep( identChar | digit )              ^^ { case first ~ rest => processIdent(first :: rest mkString "") }
    | digit ~ rep( digit )                              ^^ { case first ~ rest => NumericLit(first :: rest mkString "") }
    | '\'' ~ rep( chrExcept('\'', '\n', EofCh) ) ~ '\'' ^^ { case '\'' ~ chars ~ '\'' => StringLit(chars mkString "") }
    | '\"' ~ rep( chrExcept('\"', '\n', EofCh) ) ~ '\"' ^^ { case '\"' ~ chars ~ '\"' => StringLit(chars mkString "") }
    | EofCh                                             ^^^ EOF
    | '\'' ~> failure("unclosed string literal")        
    | '\"' ~> failure("unclosed string literal")        
    | delim                                             
    | failure("illegal character")
    )
  
  // legal identifier chars other than digits
  override def identChar = letter | elem('_') | elem('\'')

  override def whitespaceChar = elem("space char", ch => ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\f')

  // see `whitespace in `Scanners'
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
      "val", "def", "type", "site", "class", "include"
      //"Integer", "Boolean", "String", "Number",
      //"Signal", "Top", "Bot"
    )

  /** The set of delimiters (ordering does not matter) */
  override val delimiters = new HashSet[String] ++ List(
      "+", "-", "*", "/", "%", "**",
      "&&", "||", "~",
      "<", ">", "|", ";",
      "(", ")", "[", "]", "{", "}", ",",
      "=", "<:", ":>", ">=", "/=",
      ":", "_", "++",
      ".", "?", ":=",
      "::", ":!:" 
    )

  //TODO: These are the beginning of recognizing Java identifiers, but they are not used yet.  We need them for class decls and field access on Java sites.
  def javaIdentStartChar = elem("Java ident start char", Character.isJavaIdentifierStart(_))
  def javaIdentPartChar = elem("Java ident part char", Character.isJavaIdentifierPart(_))
  def javaIdent = javaIdentStartChar ~ rep( javaIdentStartChar) ^^ { case first ~ rest => Identifier(first :: rest mkString "") }
  //def javaQualifiedIdent = rep1sep(javaIdent, ".") ^^ { case first ~ rest => Identifier(first :: rest mkString ".") }
}
