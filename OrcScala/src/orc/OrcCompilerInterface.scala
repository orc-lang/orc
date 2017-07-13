//
// OrcCompilerInterface.scala -- Interfaces for Orc compiler
// Project OrcScala
//
// Created by dkitchin on May 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import java.io.IOException
import orc.compile.parse.OrcInputContext
import orc.error.compiletime.CompileLogger
import orc.progress.ProgressMonitor

/** The interface from a caller to the Orc compiler
  */
trait OrcCompilerProvides[+E] {
  @throws(classOf[IOException])
  def apply(source: OrcInputContext, options: OrcCompilationOptions, compileLogger: CompileLogger, progress: ProgressMonitor): E
}

/** The interface from the Orc compiler to its environment
  */
trait OrcCompilerRequires {
  @throws(classOf[IOException])
  def openInclude(includeFileName: String, relativeTo: OrcInputContext, options: OrcCompilationOptions): OrcInputContext
  @throws(classOf[ClassNotFoundException])
  def loadClass(className: String): Class[_]
}

/** An Orc compiler
  */
trait OrcCompiler[+E] extends OrcCompilerProvides[E] with OrcCompilerRequires

