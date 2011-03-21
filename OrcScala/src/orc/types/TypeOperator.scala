//
// TypeOperator.scala -- Scala trait TypeOperator
// Project OrcScala
//
// $Id: TypeOperator.scala 2228 2010-12-07 19:13:50Z jthywissen $
//
// Created by dkitchin on Dec 1, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.types

/**
 * 
 *
 * @author dkitchin
 */
trait TypeOperator {
  def operate(ts: List[Type]): Type
}
