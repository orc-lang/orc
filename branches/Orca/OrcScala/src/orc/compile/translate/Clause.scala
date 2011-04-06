//
// Clause.scala -- Scala class Clause
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 3, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import orc.ast.ext._
import orc.error.OrcExceptionExtension._
import orc.ast.oil.named
import orc.compile.translate.PrimitiveForms._
import scala.collection.immutable._
import orc.error.compiletime._

case class Clause(formals: List[Pattern], body: Expression) extends orc.ast.AST {
	
  val arity = formals.size
	
  /**
   * 
   * Convert a clause into a cascading match; if the clause patterns match,
   * execute the body, otherwise execute the fallthrough expression.
   * 
   * The supplied args are the formal parameters of the overall function.
   * 
   */
  def convert(args: List[named.BoundVar], 
              fallthrough: named.Expression) 
             (implicit 
              context: Map[String, named.Argument], 
              typecontext: Map[String, named.Type],
              translator: Translator
             ): named.Expression = {		

    import translator._
    
    /* Ensure that patterns are linear even across multiple arguments of a clause */
    var varNames: Set[String] = Set.empty
    def mentioned(name: String) {
      if (varNames contains name) {
        reportProblem(NonlinearPatternException(name) at this)
      }
      else {
        varNames = varNames + name
      }
    }
    
    
    var targetConversion: Conversion = id
    var targetContext: Map[String, named.Argument] = HashMap.empty

    val (strictPairs, nonstrictPairs) = { 
      val zipped: List[(Pattern, named.BoundVar)] = formals zip args 
      zipped partition { case (p,_) => p.isStrict }
    }

    for ((p,x) <- nonstrictPairs) {
      val (source, dcontext, target) = convertPattern(p, x)
      targetConversion = targetConversion andThen target
      targetContext = targetContext ++ dcontext
      for (name <- dcontext.keys) { mentioned(name) }
    }

    strictPairs match {
      /* 
       * There are no strict patterns. 
       * There is no possibility of a failed match, so we just ignore the fallthrough case.
       */
      case Nil => {
        // Make sure the remaining cases are not redundant.
        fallthrough match {
          case named.Stop() => {  }
          case _ => { reportProblem(RedundantMatch() at fallthrough) }
        }
      }
      /* 
       * There is exactly one strict pattern.
       */
      case (strictPattern, strictArg) :: Nil => {
        val x = new named.BoundVar()
        val (source, dcontext, target) = convertPattern(strictPattern, x)
        for (name <- dcontext.keys) { mentioned(name) }
        val src = source(strictArg)
        targetContext = targetContext ++ dcontext
        targetConversion = targetConversion andThen target andThen { makeMatch(src, x, _, fallthrough) }                  
      }
      /*
       * There are multiple strict patterns.
       */
      case _ => { 
        val (strictPatterns, strictArgs) = strictPairs.unzip
        val x = new named.BoundVar()
        val (source, dcontext, target) = convertPattern(TuplePattern(strictPatterns), x)
        for (name <- dcontext.keys) { mentioned(name) }
        val src = source(makeTuple(strictArgs))
        targetContext = targetContext ++ dcontext
        targetConversion = targetConversion andThen target andThen { makeMatch(src, x, _, fallthrough) }
      }
    }  

    /* Finally, construct the new expression */
    val newbody = translator.convert(body)(context ++ targetContext, typecontext)
    this ->> targetConversion(newbody)
  }

}

object Clause {

  /**
   * If these clauses all have the same arity, return that arity.
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

  /**
   * Convert a list of clauses to a single expression
   * which linearly matches those clauses.
   * 
   * Also return the list of arguments against which the
   * converted body expression performs this match.
   * 
   * The list of clauses is assumed to be nonempty.
   */
  def convertClauses(clauses: List[Clause])
                    (implicit
                     context: Map[String, named.Argument], 
                     typecontext: Map[String, named.Type],
                     translator: Translator
                    ): (List[named.BoundVar], named.Expression) = {		
	val arity = commonArity(clauses)
	val args = (for (_ <- 0 until arity) yield new named.BoundVar()).toList

	val nil: named.Expression = named.Stop()
	def cons(clause: Clause, fail: named.Expression) = clause.convert(args, fail)
	val body = clauses.foldRight(nil)(cons)

	(args, body)
  }
	
}