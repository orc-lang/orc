//
// Error.scala -- Scala object Error
// Project OrcScala
//
// Created by jthywiss on Dec 16, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.util

import orc.error.runtime.ProgramSignalledError
import orc.types.{ Bot, SimpleFunctionType, StringType }
import orc.values.sites.{ TotalSite1Simple, TypedSite }

/** The Error site throws an Orc runtime exception with a program supplied message.
  *
  * @author jthywiss
  */
object Error extends TotalSite1Simple[String] with TypedSite {
  def eval(s: String) = {
    throw new ProgramSignalledError(s)
  }

  def orcType = SimpleFunctionType(StringType, Bot)
}
