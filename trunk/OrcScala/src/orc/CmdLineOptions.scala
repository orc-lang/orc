//
// CmdLineOptions.scala -- Scala class CmdLineOptions
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

import orc.script.OrcBindings
import scala.collection.JavaConversions._

/**
 * An OrcOptions that parses command line arguments passed with <code>parseCmdLine(args)</code>.
 *
 * @author jthywiss
 */
trait CmdLineOptions extends OrcOptions with CmdLineParser {
  StringOprd(()=>filename, filename=_, position = 0, argName = "file", required = true, usage = "Path to script to execute.")

  IntOpt(()=>debugLevel, debugLevel=_, 'd', "debug", usage = "Enable debugging output, which is disabled by default. Higher numbers increase level of detail.")

  UnitOpt(()=>(!usePrelude), ()=>usePrelude=false, ' ', "noprelude", usage = "Do not implicitly include standard library (prelude), which is included by default.")

  StringListOpt(()=>includePath, includePath=_, 'I', "include-path", usage = "Set the include path for Orc includes (same syntax as CLASSPATH). Default is \".\", the current directory. Prelude files are always available for include regardless of this setting.")  

  UnitOpt(()=>exceptionsOn, ()=>exceptionsOn=true, ' ', "exceptions", usage = "Enable exceptions (experimental), which is disabled by default.")

  UnitOpt(()=>typecheck, ()=>typecheck=true, ' ', "typecheck", usage = "Enable typechecking, which is disabled by default.")

  IntOpt(()=>maxPublications, maxPublications=_, ' ', "pub", usage = "Terminate the program after this many values are published. Default=infinity.")

  StringListOpt(()=>classPath, classPath=_, ' ', "cp", usage = "Set the class path for Orc sites (same syntax as CLASSPATH). This is only used for classes not found in the Java VM classpath.")

  //UnitOpt({=true}, 'c', "noexecute", usage = "Compile this program, but do not run it.")
  //FileOpt(, 'o', "oilOut", usage = "Write the compiled OIL to the given filename.")
  //IntOpt(, ' ', "numSiteThreads", usage = "Use up to this many threads for blocking site calls. Default=2.")
}
