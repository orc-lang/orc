//
// Utilities.scala -- Utility classes for ToJava runtime
// Project OrcScala
//
// Created by amp on Dec 6, 2014.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.tojava

import orc.CmdLineOptions
import orc.script.OrcBindings
import orc.util.{ CmdLineUsageException, PrintVersionAndMessageException }

/** A subclass of the command line parser which provides a method for calling
  * from Java 8.
  */
class OrcCmdLineOptions() extends OrcBindings() with CmdLineOptions {
  @throws(classOf[PrintVersionAndMessageException])
  @throws(classOf[CmdLineUsageException])
  def parseRuntimeCmdLine(args: Array[String]) {
    parseCmdLine(args.toSeq :+ "UNUSED FILE ARGUMENT")
  }
}
