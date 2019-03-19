//
// ExecutionLogOutputStream.scala -- Scala object ExecutionLogOutputStream
// Project OrcScala
//
// Created by jthywiss on Sep 29, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.io.{ IOException, OutputStream }
import java.lang.management.ManagementFactory
import java.nio.file.{ Files, Path, Paths, StandardOpenOption }
import java.util.regex.Pattern

/** Factory for execution log OutputStreams.
  *
  * @author jthywiss
  */
object ExecutionLogOutputStream {
  private object OrcUtilLogger extends orc.util.Logger("orc.util")

  /** Create the output directory for ExecutionLogOutputStream if needed.
    *
    * This can be safely called when orc.executionlog.dir is unset.
    *
    * This function is a utility to avoid "No such file or directory" errors
    * when trying to open an output in cases where the test harness does not
    * create the output directory ahead of time.
    */
  def createOutputDirectoryIfNeeded(): Unit = {
    val out = System.getProperty("orc.executionlog.dir")
    if (out != null) {
      val outDir = Paths.get(out)
      if (Files.notExists(outDir)) {
        Files.createDirectories(outDir)
        OrcUtilLogger.warning("Created output directory: " + outDir.toAbsolutePath)
      }
    }
  }

  /** Creates Some(OutputStream) with the given basename.extension if the
    * orc.executionlog.dir system property is set, else returns None. The
    * caller must close the returned stream.
    *
    * If the orc.executionlog.fileprefix system property is set, the given
    * basename is prefixed with it, otherwise no prefix is used.
    *
    * If the orc.executionlog.filesuffix system property is set, the given
    * basename is suffixed with it, otherwise no suffix is used. The string
    * "${jvmName}" in the suffix is replaced by the JVM's name. (This is
    * usually a combination of process ID and node name.)
    */
  @throws[IOException]
  def apply(basename: String, extension: String, description: String): Option[OutputStream] = {
    getFile(basename, extension) map { Files.newOutputStream(_, StandardOpenOption.CREATE_NEW) }
  }

  def getFile(basename: String, extension: String): Option[Path] = {
    val outDir = System.getProperty("orc.executionlog.dir")
    if (outDir != null) {
      val fileBasenamePrefix = System.getProperty("orc.executionlog.fileprefix", "")
      val fileBasenameSuffix = Pattern.compile("${jvmName}", Pattern.LITERAL).matcher(System.getProperty("orc.executionlog.filesuffix", "")).replaceAll(ManagementFactory.getRuntimeMXBean.getName)
      val outFile = Paths.get(outDir, s"$fileBasenamePrefix$basename$fileBasenameSuffix.$extension")
      Some(outFile)
    } else {
      None
    }
  }
}
