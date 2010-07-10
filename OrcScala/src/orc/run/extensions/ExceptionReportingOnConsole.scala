//
// ExceptionReportingOnConsole.scala -- Scala class/trait/object ExceptionReportingOnConsole
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
import orc.error.OrcException

/**
 * 
 *
 * @author dkitchin
 */
trait ExceptionReportingOnConsole extends OrcRuntime {
  def caught(e: Throwable) { 
    e match {
      case (ex: OrcException) => println(ex.getPosition().longString) 
    }
    e.printStackTrace() 
  }
}


