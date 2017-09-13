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

/** A map of settings loaded from configuration files.
  *  
  * Rule of thumb for configuration files vs. command line arguments:
  * Use configuration files for administrative values that do not change
  * frequently, i.e. general attributes of providing services.  Conceptually,
  * configuration settings are read-only for "ordinary" end users.  On the
  * other hand, use command line arguments for values that correlate with
  * particular input (Orc programs in our case).
  *
  * The files contain string key value pairs in the Java properties file
  * format (see the cross-reference below for the format).
  *
  * The directories listed in the given system property ("orc.config.dirs"
  * by default) are searched for the given filename.  Settings in files found
  * earlier in the list of directories take precedence.  If the system
  * property is not set, then no directory-list-based search is performed.
  * In any case, defaults are loaded from a resource on the class path,
  * namely a file of the given name in the package of the concrete subclass
  * of this class.
  *
  * @see java.util.Properties#load(java.io.Reader)
  * @author jthywiss
  */
abstract class Config(val filename: String, val systemProperty: String = "orc.config.dirs") extends scala.collection.immutable.AbstractMap[String,String] with scala.collection.immutable.DefaultMap[String, String]() {

  protected val settings: Properties = load()

  override def toString: String = iterator.map(e => e._1 + "=" + e._2).mkString(s"${getClass.getName}(#filename=$filename; ", ", ", ")")

  protected def load(): Properties = {
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
    val configDirProp = System.getProperty(systemProperty)
    if (configDirProp != null && configDirProp.nonEmpty) {
      val configDirs = configDirProp.split(File.pathSeparatorChar)
      wrapProps(configDirs.reverseIterator,defaults)
    } else {
      defaults
    }
  }

  override def get(key: String): Option[String] = {
    val v = settings.getProperty(key)
    if (v != null)
      Some(v)
    else if (settings.containsKey(key))
      Some(null.asInstanceOf[String])
    else
      None
  }

  override def iterator: Iterator[(String, String)] = new scala.collection.AbstractIterator[(String, String)]() {
    val si = settings.stringPropertyNames.iterator()
    def hasNext = si.hasNext
    def next() = { val k = si.next(); (k, settings.getProperty(k)) }
  }

}
