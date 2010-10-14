//
// CurriedForms.scala -- Scala object CurriedForms
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 5, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.translate

import orc.ast.ext

/**
 * 
 *
 * @author srosario, dkitchin
 */
object CurriedForms {

  /**
   * Converts a definition 
   * 
   * def f(x1,..xn)(y1,..yn)(..) = body
   * 
   * to 
   * 
   * def f(x1,..xn) = lambda(y1,..yn) (lambda(...)..) body
   * 
   */
  def reduceParamLists(d: ext.DefDeclaration): ext.DefDeclaration = {
    import orc.error.compiletime.typing._
    d -> {
      case ext.Def(_, List(formals), _, _, _) => d
      case ext.Def(name, formals :: tail, retType, guard, body) => {
        val newbody = uncurry(tail, body, retType, guard)
        /* Return the outermost Def */
        ext.Def(name, List(formals), None, None, newbody)
      }
      case ext.DefClass(name, formals, retType, guard, body) => {
        reduceParamLists(ext.Def(name, formals, retType, guard, ext.DefClassBody(body)))
      }
      case ext.DefSig(name, typFormals, List(argTypes), retType) => d
      case ext.DefSig(name, typFormals, argTypes :: tail, retType) => {
        val lambdaType = ext.LambdaType(Nil, tail, retType)
        val newRetType = lambdaType.cut
        ext.DefSig(name, typFormals, List(argTypes), newRetType)
      }
      case ext.DefSig(_, _, List(), _) =>
        throw new UnspecifiedArgTypesException()
    }
  }

  /**
   * Given formals = (x1,..xn)(y1,..yn)(..)
   * builds the expression
   *   lambda(x1,..xn) = (lambda(y1,..yn) = (lambda(...) = .. body ))
   */
  def uncurry(formals: List[List[ext.Pattern]], body: ext.Expression, retType: Option[ext.Type], guard: Option[ext.Expression]): ext.Lambda = {

    def makeLambda(body: ext.Expression, params: List[ext.Pattern]) =
      ext.Lambda(None, List(params), None, None, body)

    val revFormals = formals reverse
    /* Inner most lambda has the return type of the curried definition */
    val innerLambda = ext.Lambda(None, List(revFormals head), retType, guard, body)
    /* Make new Lambda expressions, one for each remaining list of formals */
    revFormals.tail.foldLeft(innerLambda)(makeLambda)
  }

  def reduceParamLists(e: ext.Lambda): ext.Lambda = {
    e -> {
      case ext.Lambda(_, List(formals), _, _, _) => e
      case ext.Lambda(typFormals, formals :: tail, retType, guard, body) => {
        val newbody = uncurry(tail, body, retType, guard)
        ext.Lambda(typFormals, List(formals), None, None, newbody)
      }
    }
  }

}
