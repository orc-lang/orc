//
// ASTWithIndex.scala -- Scala trait ASTWithIndex
// Project OrcScala
//
// Created by amp on Aug 11, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast

trait ASTWithIndex extends ASTForSwivel {
  var optionalIndex: Option[Int] = None
}