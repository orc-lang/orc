//
// StackUtils.scala -- Scala class/trait/object StackUtils
// Project OrcScala
//
// $Id$
//
// Created by amp on Nov 9, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.util

import java.io.StringWriter
import java.io.PrintWriter

/**
  *
  * @author amp
  */
object StackUtils {
  def getStack(): String = {
    val e = new Throwable("Stack Capture")
    e.setStackTrace(e.getStackTrace().tail)
    getStack(e)
  }
  def getStack(e: Throwable): String = {
    val result = new StringWriter()
    val printWriter = new PrintWriter(result)
    e.printStackTrace(printWriter)
    val s = result.toString()
    s.substring(s.indexOf('\n')+1)
  }
}