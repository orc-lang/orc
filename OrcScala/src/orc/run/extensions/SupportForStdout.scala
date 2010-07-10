//
// SupportForStdout.scala -- Scala class/trait/object SupportForStdout
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.OrcRuntime

/**
 * 
 *
 * @author dkitchin
 */
trait SupportForStdout extends OrcRuntime {
  def printToStdout(s: String) { print(s) }
}
