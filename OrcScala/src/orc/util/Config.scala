//
// Config.scala -- Scala class Config
// Project OrcScala
//
// Created by jthywiss on Sep 11, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.util

import java.io.{ File, FileInputStream }
import java.util.Properties

/** A map of settings loaded from configuration files.  The directories
  * listed in the given system property ("orc.config.dirs" by default)
  * are searched for the given filename.  Settings in files found
  * earlier in the list of directories take precedence.  Defaults are
  * loaded from a resource on the class path, namely a file of the given
  * name in the package of the concrete subclass of this class.
  *
  * @author jthywiss
  */
abstract class Config(filename: String, systemProperty: String = "orc.config.dirs") extends scala.collection.immutable.AbstractMap[String,String] with scala.collection.immutable.DefaultMap[String, String]() {

  protected val settings = load()

  override def toString = iterator.map(e => e._1 + "=" + e._2).mkString(s"${getClass.getName}(# filename=$filename; ", ", ", ")")

  protected def load() = {
    def wrapProps(dirs: Iterator[String], inner: Properties): Properties = {
      if (dirs.hasNext) {
        val f = new File(dirs.next(), filename)
        if (f.exists) {
          val wrapper = new Properties(inner)
          wrapper.load(new FileInputStream(f))
          wrapProps(dirs, wrapper)
        } else {
          wrapProps(dirs, inner)
        }
      } else {
        inner
      }
    }

    val defaultsStream = getClass.getResourceAsStream(filename)
    val defaults = if (defaultsStream == null) {
      new Properties()
    } else {
      val defaults = new Properties()
      defaults.load(defaultsStream)
      defaults
    }
    val configDirs = System.getProperty(systemProperty).split(File.pathSeparatorChar)
    wrapProps(configDirs.reverseIterator,defaults)
  }

  override def get(key: String) = {
    val v = settings.getProperty(key)
    if (v != null)
      Some(v)
    else if (settings.containsKey(key))
      Some(null.asInstanceOf[String])
    else
      None
  }

  def iterator: Iterator[(String, String)] = new scala.collection.AbstractIterator[(String, String)]() {
    val si = settings.stringPropertyNames.iterator()
    def hasNext = si.hasNext
    def next() = { val k = si.next(); (k, settings.getProperty(k)) }
  }

}
