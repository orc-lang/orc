//
// Error.scala -- Scala object Error
// Project OrcScala
//
// $Id: Error.scala 2773 2011-04-20 01:12:36Z jthywissen $
//
// Created by jthywiss on Dec 16, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.util

import orc.values.sites.TotalSite
import orc.values.sites.TotalSite1
import orc.values.sites.TypedSite
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ProgramSignalledError
import orc.types.Bot
import orc.types.SimpleFunctionType
import orc.types.StringType

/**
 * The Error site throws an Orc runtime exception with a program supplied message.
 *
 * @author jthywiss
 */
object Error extends TotalSite with TotalSite1 with TypedSite {
  def eval(x: AnyRef) = {
    x match {
      case s: String => throw new ProgramSignalledError(s)
      case _ => throw new ArgumentTypeMismatchException(0, "String", if (x != null) Option(x.getClass.getCanonicalName).getOrElse(x.getClass.getName) else "null")
    }
  }
  
  def orcType = SimpleFunctionType(StringType, Bot)
}
