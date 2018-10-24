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

import orc.error.runtime.{ ArgumentTypeMismatchException, ProgramSignalledError }
import orc.types.{ Bot, SimpleFunctionType, StringType }
import orc.values.sites.{ LocalSingletonSite, TypedSite }
import orc.values.sites.compatibility.{ TotalSite, TotalSite1 }

/** The Error site throws an Orc runtime exception with a program supplied message.
  *
  * @author jthywiss
  */
object Error extends TotalSite with TotalSite1 with TypedSite with Serializable with LocalSingletonSite {
  def eval(x: AnyRef) = {
    x match {
      case s: String => throw new ProgramSignalledError(s)
      case _ => throw new ArgumentTypeMismatchException(0, "String", orc.util.GetScalaTypeName(x))
    }
  }

  def orcType = SimpleFunctionType(StringType, Bot)
}
