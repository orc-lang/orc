//
// SiteClassLoading.scala -- Scala trait and object SiteClassLoading
// Project OrcScala
//
// Created by jthywiss on Jul 26, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import scala.collection.JavaConverters._

import java.io.File
import java.net.URL
import java.net.URLClassLoader

import orc.util.FirstNonNull

/** Provides a loadClass/getResource implementation that uses the Java class path,
  * plus the class path supplied to SiteClassLoading.initWithClassPath.
  *
  * @author jthywiss
  */
trait SiteClassLoading {
  @throws(classOf[ClassNotFoundException])
  def loadClass(name: String) = SiteClassLoading.classLoader.loadClass(name)
  def getResource(name: String) = SiteClassLoading.classLoader.getResource(name)
}

object SiteClassLoading {
  /*
   * N.B. -- Given that:
   *  1) Classes are identified by the combination of their loading
   *     class loader and their name.
   *  2) URLClassLoader cannot change its class path once constructed.
   *  3) Classes cannot be unloaded from a JVM.
   * We have the following alternatives:
   *  1) Permit arbitrary class loaders to be created here, and
   *     possibly end up with multiply loaded classes.
   *  2) Allow only additional (stacked) class loaders to be created
   *     here, which precludes reducing the class path after startup.
   * We've chosen 2, because 1 would violate singletons' invariant.
   *
   * We allow double-init so that testing works.
   */

  private var classLoader = FirstNonNull(
    Thread.currentThread().getContextClassLoader(),
    getClass().getClassLoader(),
    ClassLoader.getSystemClassLoader())

  def initWithClassPathStrings(classPath: java.util.List[String]) { initWithClassPathStrings(classPath.asScala.map(identity).toArray) }

  def initWithClassPathStrings(classPath: Array[String]) { initWithClassPathUrls(classPath.map(path2URL(_)).toArray) }

  def initWithClassPathUrls(classPath: Array[URL]) {
    Logger.config("Initializing site & class loading with class path " + classPath.mkString(":"))
    stackClassLoaderWithPath(classPath)
  }

  private def path2URL(path: String): URL = {
    // The same logic as the AppClassLoader uses to parse system.class.path
    var canFile = new File(if (path.length == 0) "." else path)
    try {
      canFile = canFile.getCanonicalFile
    } catch {
      case e: InterruptedException => throw e
      case e: Exception => {}
    }
    canFile.toURI.toURL
  }

  private def stackClassLoaderWithPath(classPath: Array[URL]) {
    if (classPath != null && classPath.length > 0) {
      classLoader = URLClassLoader.newInstance(classPath, classLoader)
    } else {
      // Leave alone
    }
  }
}
