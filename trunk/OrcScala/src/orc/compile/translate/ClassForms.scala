//
// ClassForms.scala -- Scala object ClassForms
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 5, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
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
object ClassForms {

  /** 
   * Helper functions for class conversion
   */
  def makeClassBody(body: ext.Expression,
                      reportProblem: CompilationException with ContinuableSeverity => Unit
                     ): ext.Expression = makeClassBody(body, Nil, reportProblem)

  def makeClassBody(body: ext.Expression, 
                      defNames: List[String],
                      reportProblem: CompilationException with ContinuableSeverity => Unit
                     ): ext.Expression = {
    body match {
      case ext.Declare(decl: ext.DefDeclaration, e) => {
        return new ext.Declare(decl, makeClassBody(e, decl.name :: defNames, reportProblem))
      }
      case ext.Declare(decl, e) => {
        return new ext.Declare(decl, makeClassBody(e, defNames, reportProblem))
      }
      case _ => {}
    }
    val dNames = defNames.distinct
    val members =
      for (d <- dNames) yield {
        val call = new ext.Call(new ext.Constant(builtin.MakeSite), List(ext.Args(None, List(new ext.Variable(d)))))
        (d, call)
      }
    val classRecord = new ext.RecordExpr(members.toList)
    ext.Parallel(ext.Sequential(body, None, ext.Stop()), classRecord)
  }

}
