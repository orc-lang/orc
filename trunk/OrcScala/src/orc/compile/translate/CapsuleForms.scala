//
// CapsuleForms.scala -- Scala class/trait/object CapsuleForms
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
import orc.lib.builtin
import orc.error.compiletime._
import orc.error.OrcExceptionExtension._

/**
 * 
 *
 * @author dkitchin
 */
object CapsuleForms {

  /** 
   * Helper functions for capsule conversion
   */
  def makeCapsuleBody(body: ext.Expression,
                      reportProblem: CompilationException with ContinuableSeverity => Unit
                     ): ext.Expression = makeCapsuleBody(body, Nil, reportProblem)

  def makeCapsuleBody(body: ext.Expression, 
                      defNames: List[String],
                      reportProblem: CompilationException with ContinuableSeverity => Unit
                     ): ext.Expression = {
    body match {
      case ext.Declare(decl: ext.DefDeclaration, e) => {
        return new ext.Declare(decl, makeCapsuleBody(e, decl.name :: defNames, reportProblem))
      }
      case ext.Declare(decl, e) => {
        return new ext.Declare(decl, makeCapsuleBody(e, defNames, reportProblem))
      }
      case _ => {}
    }
    val dNames = defNames.distinct
    if (dNames.isEmpty) {
      throw (DeflessCapsule() at body)
    }
    val recordCall: ext.Call = new ext.Call(new ext.Constant(builtin.RecordConstructor), List(ext.Args(None, makeRecordArgs(dNames))))
    ext.Parallel(ext.Sequential(body, None, ext.Stop()), recordCall)
  }

  /**
   * Builds a list of Tuples (def-name,site-call) for every 
   * definition name in the input list.
   */
  def makeRecordArgs(defNames: List[String]): List[ext.Expression] = {
    var args: List[ext.Expression] = Nil
    for (d <- defNames) {
      val call = new ext.Call(new ext.Constant(builtin.SiteSite), List(ext.Args(None, List(new ext.Variable(d)))))
      val tuple = ext.TupleExpr(List(new ext.Constant(d), call))
      args = args ::: List(tuple)
    }
    args
  }

}