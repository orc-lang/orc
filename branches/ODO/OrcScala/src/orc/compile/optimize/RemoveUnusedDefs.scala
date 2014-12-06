//
// RemoveUnusedDefs.scala -- Scala object RemoveUnusedDefs
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 12, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.optimize

import scala.language.postfixOps
import orc.ast.oil.named.{ BoundTypevar, BoundVar, DeclareCallables, Callable, NamedASTTransform }

/** Removes unused definitions from the OIL AST.
  *
  * This optimization is more useful if it occurs after FractionDefs.
  *
  * @author dkitchin
  */
object RemoveUnusedDefs extends NamedASTTransform {

  override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
    case DeclareCallables(defs, body) => {
      val defnames = defs map { _.name }
      val newbody = transform(body, defnames ::: context, typecontext)
      val defNamesSet: Set[BoundVar] = defs.toSet map ((a: Callable) => a.name)

      // If none of the defs are bound in the body,
      // just return the body.
      if (newbody.freevars intersect defNamesSet isEmpty) {
        newbody
      } else {
        val newdefs = defs map { transform(_, defnames ::: context, typecontext) }
        DeclareCallables(newdefs, newbody)
      }
    }
  }
}
