//
// CustomParserCombinators.scala -- Scala trait CustomParserCombinators
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Sep 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.parse

import scala.util.parsing.combinator.syntactical._
import orc.ast.ext.Expression
import orc.ast.ext.InfixOperator
import orc.ast.AST

/**
 *
 * An assortment of extended parser combinators designed specifically
 * for the Orc parser.
 *
 * @author dkitchin
 */
trait CustomParserCombinators {

  self: StandardTokenParsers =>

  ////////
  // Preserve input source positions
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
      markLocation(parser ^^ { case x ~ y ~ z ~ u => f(x,y,z,u) })
  }
  class Maps5[A,B,C,D,E](parser: Parser[A ~ B ~ C ~ D ~ E]) {
    def ->[X <: AST](f: (A,B,C,D,E) => X): Parser[X] =
      markLocation(parser ^^ { case x ~ y ~ z ~ u ~ v => f(x,y,z,u,v) })
  }
  class Maps6[A,B,C,D,E,F](parser: Parser[A ~ B ~ C ~ D ~ E ~ F]) {
    def ->[X <: AST](f: (A,B,C,D,E,F) => X): Parser[X] =
      markLocation(parser ^^ { case x ~ y ~ z ~ u ~ v ~ w => f(x,y,z,u,v,w) })
  }

  implicit def CreateMaps0Parser(s: String): Maps0 = new Maps0(s)
  implicit def CreateMaps1Parser[A](parser: Parser[A]): Maps1[A] = new Maps1(parser)
  implicit def CreateMaps2Parser[A,B](parser: Parser[A ~ B]): Maps2[A,B] =  new Maps2(parser)
  implicit def CreateMaps3Parser[A,B,C](parser: Parser[A ~ B ~ C]): Maps3[A,B,C] = new Maps3(parser)
  implicit def CreateMaps4Parser[A,B,C,D](parser: Parser[A ~ B ~ C ~ D]): Maps4[A,B,C,D] = new Maps4(parser)
  implicit def CreateMaps5Parser[A,B,C,D,E](parser: Parser[A ~ B ~ C ~ D ~ E]): Maps5[A,B,C,D,E] = new Maps5(parser)
  implicit def CreateMaps5Parser[A,B,C,D,E,F](parser: Parser[A ~ B ~ C ~ D ~ E ~ F]): Maps6[A,B,C,D,E,F] = new Maps6(parser)

  ////////
  // Extended apply combinator -?->
  ////////

  class Maps1Optional2[A <: AST,B](parser: Parser[A ~ Option[B]]) {

  def -?->(f: (A,B) => A): Parser[A] =
      markLocation(parser ^^ {
        case x ~ None => x
        case x ~ Some(y) => f(x,y)
      })
  }

  implicit def CreateMaps1Optional2Parser[A <: AST,B](parser: Parser[A ~ Option[B]]): Maps1Optional2[A,B] = new Maps1Optional2(parser)

  ////////
  // Interleaving combinator
  ///////

  class InterleavingParser[A <: AST](parser: Parser[A]) {
    def leftInterleave[B](interparser: Parser[B]) =
      (f: (A,B,A) => A) => {
        def origami(b: B)(x:A, y:A): A = f(x,b,y)
        markLocation( chainl1(markLocation(parser), interparser ^^ origami) )
      }: Parser[A]
    def rightInterleave[B](interparser: Parser[B]) =
      (f: (A,B,A) => A) => {
        def origami(b: B)(x:A, y:A): A = f(x,b,y)
        markLocation( chainr1(markLocation(parser), interparser ^^ origami) )
      }: Parser[A]
  }

  implicit def CreateInterleavingParser[A <: AST](parser: Parser[A]): InterleavingParser[A] = new InterleavingParser(parser)


  ////////
  // Infixing combinator
  ///////

  class InfixingParser(parser: Parser[Expression]) {

    def opsParser(ops: List[String]): Parser[String] = {
      ops map keyword reduceRight { _ | _ }
    }

    def stageInfixOp(op: String)(left: Expression, right: Expression): Expression = {
      InfixOperator(left, op, right)
    }

    def leftAssociativeInfix(ops: List[String]): Parser[Expression] = {
      chainl1(parser, opsParser(ops) ^^ stageInfixOp)
    }

    def rightAssociativeInfix(ops: List[String]): Parser[Expression] = {
      chainr1(parser, opsParser(ops) ^^ stageInfixOp)
    }

    def nonAssociativeInfix(ops: List[String]): Parser[Expression] = {
        parser ~ ((opsParser(ops) ~ parser)?) -?->
          { (a,opb) => opb match { case op ~ b => InfixOperator(a,op,b) } }
    }

    // By default, fully associative operators are associated to the left.
    def fullyAssociativeInfix(ops: List[String]) = leftAssociativeInfix(ops)
  }

  implicit def CreateInfixingParser[A <: Expression](parser: Parser[A]): InfixingParser = new InfixingParser(parser)

  ////////
  // Our own chainl1 and chainr1
  ////////

  override def chainl1[T](p: => Parser[T], q: => Parser[(T, T) => T]): Parser[T] = {
    val markingQ = markingParser(q)
    p ~ rep(markingQ ~ p) ^^
      { case x ~ xs =>
          (xs foldLeft x) { (a,fb) => fb match { case f ~ b => f(a,b) } }
      }
  }

  def chainr1[T](p: => Parser[T], q: => Parser[(T, T) => T]): Parser[T] = {
    def rightChainFold(list: List[((T, T) => T) ~ T]): (T => T) = {
      list match {
        case List(f ~ a) => f(_,a)
        case f ~ a :: xs => f(_,rightChainFold(xs)(a))
      }
    }
    val markingQ = markingParser(q)
    p ~ rep(markingQ ~ p) ^^ {case x ~ xs => if (xs.isEmpty) x else rightChainFold(xs)(x)}
  }

  ////////
  //
  ////////

  def markingParser[T](q: => Parser[(T, T) => T]) = {
    new Parser[(T, T) => T] {
      override def apply(i: Input) = {
        val position = i.pos
        val result: ParseResult[(T,T) => T] = q.apply(i)
        result map
          { (f: (T,T) => T) =>
            { (a: T, b: T) =>
              {
                val result = f(a,b)
                result match {
                  case ast : AST => ast.pos = position
                }
                result
              }
            }
          }
      }
    }
  }

}
