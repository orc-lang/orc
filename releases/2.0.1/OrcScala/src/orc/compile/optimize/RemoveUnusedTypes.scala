//
// RemoveUnusedTypes.scala -- Scala object RemoveUnusedTypes
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 12, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.optimize

import orc.ast.oil.named._

/**
 * 
 * Remove unused type declarations from the AST.
 * 
 * @author dkitchin
 */
object RemoveUnusedTypes extends NamedASTTransform {
  
  override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
    case DeclareType(u, t, body) => {
      val newbody = transform(body, context, u :: typecontext)
      if (newbody.freetypevars contains u)
        { DeclareType(u, t, newbody) }
      else
        { newbody }
    }
  }
  
}
