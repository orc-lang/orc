//
// DocParsers.scala -- Scala object DocParsers
// Project OrcDocgen
//
// Created by dkitchin on Dec 16, 2010.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package docgen

import java.nio.file.{ Files, Path }

import scala.language.postfixOps
import scala.util.parsing.combinator.RegexParsers

/**
  *
  * @author dkitchin
  */
object DocParsers extends RegexParsers {

  // Do not discard any whitespace
  override val whiteSpace = """""".r

  val beginDoc = """\{--""".r
  val endDoc = """--\}""".r

  val blankLine = """[ \t]*\n""".r
  val padding = """\s*""".r

  def getLeadingName(line: String) = {
    // Note: matches only letter/number/_ identifiers. Does not yet handle Unicode identifiers.
    """\s*\w+""".r.findPrefixOf(line) match {
      case Some(ident) => ident.trim()
      case None => {
        """\s*\(0?[+*/%&|~<>=:.?\-]+\)""".r.findPrefixOf(line) match {
          case Some(opname) => opname.trim()
          case None => ""
        }
      }
    }

  }

  def parseFullLine: Parser[String] = """[^\n]+""".r <~ "\n"

  def parseFile(f: Path): List[DocItem] = {
    val reader = scala.util.parsing.input.StreamReader(Files.newBufferedReader(f))
    phrase(parseDocument)(reader) match {
      case Success(items, _) => items
      case _ => throw new Exception("Parser failure while parsing file " + f.toString() + " in Docgen.")
    }
  }

  def parseDocument: Parser[List[DocItem]] =
    ((
      beginDoc ~> padding ~> parseDocBlock <~ padding <~ endDoc
      | parseNondocBlock ^^ { List(_) })*) ^^ { _.flatten }

  def parseDocBlock: Parser[List[DocItem]] = parseDocText ~ (parseDocItem*) ^^ { case text ~ items => text :: items }

  def parseNondocBlock: Parser[DocOutside] =
    ((
      """[^\{]+""".r
      | not(beginDoc) ~> """\{""".r)+) ^^ { chars => DocOutside(chars.mkString("")) }

  def parseDocItem: Parser[DocItem] = parseSite | parseDef | parseImp

  val siteTag = """@site""".r
  def parseSite: Parser[DocSite] =
    siteTag ~> parseFullLine ~ (parseDocText?) ~ (parseMethod*) ^^
      { case line ~ text ~ items => DocSite(getLeadingName(line), line, text.toList ::: items) }

  val defTag = """@def""".r
  def parseDef: Parser[DocDefn] =
    defTag ~> parseFullLine ~ (parseDocText?) ~ (parseImp?) ^^
      { case line ~ text ~ imp => DocDefn(getLeadingName(line), line, text.toList ::: imp.toList) }

  val impTag = """@implementation""".r
  def parseImp: Parser[DocItem] = impTag <~ blankLine ^^^ DocImpl

  val methodTag = """@method""".r
  def parseMethod: Parser[DocSite] =
    methodTag ~> parseFullLine ~ parseDocText ^^
      { case line ~ text => new DocSite(getLeadingName(line), line, List(text)) { override val keyword = "" } }

  // Note: may produce an empty string or only whitespace. We handle these cases later.
  def parseDocText =
    ((
      """[^@\-]+""".r
      | not(siteTag | defTag | impTag | methodTag | endDoc) ~> """[@\-]""".r)*) ^^ { chars => DocText(chars.mkString("")) }

}
