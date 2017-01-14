//
// Clause.scala -- Scala class Clause
// Project OrcScala
//
// Created by dkitchin on Jun 3, 2010.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import scala.collection.immutable.{ List, Map, Nil }
import scala.language.reflectiveCalls

import orc.ast.ext._
import orc.ast.oil.named
import orc.compile.translate.PrimitiveForms._
import orc.error.OrcExceptionExtension._
import orc.error.compiletime._

case class Clause(formals: List[Pattern], maybeGuard: Option[Expression], body: Expression) extends orc.ast.AST {

  val arity = formals.size

  /** Convert a clause into a cascading match; if the clause patterns match,
    * execute the body, otherwise execute the fallthrough expression.
    *
    * The supplied args are the formal parameters of the overall function.
    */
  def convert(args: List[named.BoundVar],
    fallthrough: named.Expression)(implicit ctx: TranslatorContext,
      translator: Translator): named.Expression = {

    import translator._
    import ctx._

    var targetConversion: Conversion = id
    def extendConversion(f: Conversion) {
      targetConversion = targetConversion andThen f
    }

    val targetContext: scala.collection.mutable.Map[String, named.Argument] = new scala.collection.mutable.HashMap()
    def extendContext(dcontext: Map[String, named.Argument]) {
      for ((name, y) <- dcontext) {
        /* Ensure that patterns are linear even across multiple arguments of a clause */
        if (targetContext contains name) {
          reportProblem(NonlinearPatternException(name) at this)
        } else {
          targetContext += { (name, y) }
        }
      }
    }

    /* Convert this expression with respect to the current targetContext,
     * using the current targetConversion.
     */
    def convertInContext(e: Expression): named.Expression = {
      targetConversion(translator.convert(e)(ctx.copy(context = context ++ targetContext)))
    }

    val (strictPairs, nonstrictPairs) = {
      val zipped: List[(Pattern, named.BoundVar)] = formals zip args
      zipped partition { case (p, _) => p.isStrict }
    }

    for ((p, x) <- nonstrictPairs) {
      val (_, dcontext, target) = convertPattern(p, x)
      extendConversion(target)
      extendContext(dcontext)
    }

    strictPairs match {
      /*
       * There are no strict patterns.
       */
      case Nil => {
        maybeGuard match {
          case Some(guard) => {
            // If there are no strict patterns, then we just branch on the guard.
            val newGuard = guard -> convertInContext
            extendConversion({ makeConditionalFalseOnHalt(newGuard, _, fallthrough) })
          }
          case None => {
            /*
             * If there are no strict patterns and there is no guard,
             * then the clause is unconditional. If there are any
             * subsequent clauses, they are redundant.
             */
            fallthrough match {
              case named.Stop() => {}
              case _ => { reportProblem(RedundantMatch() at fallthrough) }
            }
          }
        }
      }

      /*
       * There is at least one strict pattern.
       */
      case _ => {

        val x = new named.BoundVar()

        val (newSource, dcontext, target) =
          strictPairs match {
            case (strictPattern, strictArg) :: Nil => {
              val (source, dcontext, target) = convertPattern(strictPattern, x)
              val newSource = source(strictArg)
              (newSource, dcontext, target)
            }
            /* If there is more than one strict pattern,
             * we treat it as a single tuple pattern containing those patterns.
             */
            case _ => {
              val (strictPatterns, strictArgs) = strictPairs.unzip
              val (source, dcontext, target) = convertPattern(TuplePattern(strictPatterns), x)
              val newSource = source(makeTuple(strictArgs))
              (newSource, dcontext, target)
            }
          }

        extendContext(dcontext)
        extendConversion(target)

        val guardedSource =
          maybeGuard match {
            case Some(guard) => {
              val g = new named.BoundVar()
              val b = new named.BoundVar()
              val newGuard = convertInContext(guard).subst(g, x)
              newSource > g > (named.Graft(b, named.Trim(newGuard), callIft(b)) >> g)
            }
            case None => newSource
          }

        extendConversion({ makeMatch(guardedSource, x, _, fallthrough) })
      }
    }

    /* Finally, construct the new expression */
    this ->> convertInContext(body)
  }

}

object Clause {

  /** If these clauses all have the same arity, return that arity.
    * Otherwise, throw an exception.
    *
    * The list of clauses is assumed to be nonempty.
    */
  def commonArity(clauses: List[Clause]): Int = {
    val first :: rest = clauses

    rest find { _.arity != first.arity } match {
      case Some(clause) => throw (ClauseArityMismatch() at clause)
      case None => first.arity
    }
  }

  /** Convert a list of clauses to a single expression
    * which linearly matches those clauses.
    *
    * Also return the list of arguments against which the
    * converted body expression performs this match.
    *
    * The list of clauses is assumed to be nonempty.
    */
  def convertClauses(clauses: List[Clause])(implicit ctx: TranslatorContext,
    translator: Translator): (List[named.BoundVar], named.Expression) = {
    val arity = commonArity(clauses)
    val args = (for (_ <- 0 until arity) yield new named.BoundVar()).toList

    val nil: named.Expression = named.Stop()
    def cons(clause: Clause, fail: named.Expression) = clause.convert(args, fail)
    val body = clauses.foldRight(nil)(cons)

    (args, body)
  }

}
