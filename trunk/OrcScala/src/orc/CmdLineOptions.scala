//
// CmdLineOptions.scala -- Scala class/trait/object CmdLineOptions
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jul 16, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http: //orc.csres.utexas.edu/license.shtml .
//
package orc

import scala.collection.JavaConversions._

/**
 * An OrcOptions that parses command line arguments
 *
 * @author jthywiss
 */
class CmdLineOptions(args: Array[String]) extends OrcOptions with CmdLineParser {
  StringOprd(filename=_, position = 0, argName = "file", required = true, usage = "Path to script to execute.")
  override var filename: String = null

  override var debugLevel: Int = 0

  UnitOpt(()=>incrementDebugLevel, 'd', "debug", usage = "Enable debugging output, which is disabled by default. Repeat this argument to increase verbosity.")
  def incrementDebugLevel { debugLevel += 1 }

  UnitOpt(()=>usePrelude=false, ' ', "noprelude", usage = "Do not implicitly include standard library (prelude), which is included by default.")
  override var usePrelude: Boolean = true

  StringListOpt(includePath=_, 'I', "include-path", usage = "Set the include path for Orc includes (same syntax as CLASSPATH). Default is \".\", the current directory. Prelude files are always available for include regardless of this setting.")  
  override var includePath: java.util.List[String] = { val l = new java.util.ArrayList[String](1); l.add("."); l }

  override var additionalIncludes: java.util.List[String] = new java.util.ArrayList[String](0)

  UnitOpt(()=>exceptionsOn=true, ' ', "exceptions", usage = "Enable exceptions (experimental), which is disabled by default.")
  override var exceptionsOn: Boolean = false

  UnitOpt(()=>typecheck=true, ' ', "typecheck", usage = "Enable typechecking, which is disabled by default.")
  override var typecheck: Boolean = false

  IntOpt(maxPublications=_, ' ', "pub", usage = "Terminate the program after this many values are published. Default=infinity.")
  override var maxPublications: Int = 0

  override var tokenPoolSize: Int = 0

  override var stackSize: Int = 0

  StringListOpt(classPath=_, ' ', "-cp", usage = "Set the class path for Orc sites (same syntax as CLASSPATH). This is only used for classes not found in the Java VM classpath.")
  override var classPath: java.util.List[String] = new java.util.ArrayList[String](0)

  //UnitOpt({=true}, 'c', "noexecute", usage = "Compile this program, but do not run it.")
  //FileOpt(, 'o', "oilOut", usage = "Write the compiled OIL to the given filename.")
  //IntOpt(, ' ', "numSiteThreads", usage = "Use up to this many threads for blocking site calls. Default=2.")

  def hasCapability(capName: String): Boolean = false
  def setCapability(capName: String, newVal: Boolean) { }

  // must be after all Oprd and Opt statements
  parseCmdLine(args)
}
