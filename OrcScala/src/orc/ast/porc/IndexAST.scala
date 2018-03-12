//
// IndexAST.scala -- Scala object IndexAST
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.porc

import orc.ast.ASTWithIndex

object IndexAST {
  def apply(ast: PorcAST): Unit = {
    var i = 0
    def nextIndex(): Int = {
      val r = i
      i += 1
      r
    }
    
    def process(ast: PorcAST): Unit = {
      ast match {
        case a: ASTWithIndex =>
          a.optionalIndex = Some(nextIndex())
        case _ =>
          ()
      }
      ast.subtrees.foreach(process)
    }
    
    process(ast)
  }
}
