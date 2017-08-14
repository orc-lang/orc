//
// Logger.scala -- Scala object Logger
// Project PorcE
//
// Created by jthywiss on Nov 14, 2015.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.distrib

/** Logger for the orc.lib.dorc subsystem.
  *
  * @author jthywiss
  */
object Logger extends orc.util.Logger("orc.run.porce.distrib") {
  Logger.julLogger.setUseParentHandlers(false)
  Logger.julLogger.setLevel(java.util.logging.Level.FINEST)
  val ch = new java.util.logging.ConsoleHandler()
  ch.setLevel(java.util.logging.Level.ALL)
  ch.setFormatter(orc.util.SyslogishFormatter)
  Logger.julLogger.addHandler(ch)
}