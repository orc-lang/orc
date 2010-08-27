//
// OrcBindings.scala -- Scala class OrcBindings
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script

import java.util.Map
import java.io.File

import javax.script.SimpleBindings

import orc.OrcOptions

/**
 * An extended implementation of <code>javax.script.Bindings</code>
 * with type-specific get and put methods.
 *
 * @author jthywiss
 */
class OrcBindings(m: Map[String, Object]) extends SimpleBindings(m) with OrcOptions {
  import scala.collection.JavaConversions._

  def this() = this(new java.util.HashMap[String,Object])

  def filename: String = getString("javax.script.filename", "")
  def filename_=(newVal: String) = putString("javax.script.filename", newVal)
  def logLevel: String = getString("orc.logLevel", "INFO")
  def logLevel_=(newVal: String) = putString("orc.logLevel", newVal)

  // Compile options
  def usePrelude: Boolean = getBoolean("orc.usePrelude", true)
  def usePrelude_=(newVal: Boolean) = putBoolean("orc.usePrelude", newVal)
  def includePath: java.util.List[String] = getPathList("orc.includePath", List("."))
  def includePath_=(newVal: java.util.List[String]) = putPathList("orc.includePath", newVal)
  def additionalIncludes: java.util.List[String] = getPathList("orc.additionalIncludes", List())
  def additionalIncludes_=(newVal: java.util.List[String]) = putPathList("orc.additionalIncludes", newVal)
  def exceptionsOn: Boolean = getBoolean("orc.exceptionsOn", false)
  def exceptionsOn_=(newVal: Boolean) = putBoolean("orc.exceptionsOn", newVal)
  def typecheck: Boolean = getBoolean("orc.typecheck", false)
  def typecheck_=(newVal: Boolean) = putBoolean("orc.typecheck", newVal)

  // Execution options
  def maxPublications: Int = getInt("orc.maxPublications", -1)
  def maxPublications_=(newVal: Int) = putInt("orc.maxPublications", newVal)
  def tokenPoolSize: Int = getInt("orc.tokenPoolSize", -1)
  def tokenPoolSize_=(newVal: Int) = putInt("orc.tokenPoolSize", newVal)
  def stackSize: Int = getInt("orc.stackSize", -1)
  def stackSize_=(newVal: Int) = putInt("orc.stackSize", newVal)
  def classPath: java.util.List[String] = getPathList("orc.classPath", List())
  def classPath_=(newVal: java.util.List[String]) = putPathList("orc.classPath", newVal)
  var capabilities = new java.util.HashMap[String, Boolean]()
  def hasRight(rightName: String): Boolean = {
    if (capabilities.containsKey(rightName)) {
      capabilities.get(rightName)
    } else {
      false
    }
  }
  def setRight(capName: String, newVal: Boolean) {
    capabilities.put(capName, newVal)
  }

  /**
   * @param key
   * @param value
   */
  def putString(key: String, value: String) {
    put(key, value.toString)
  }

  /**
   * @param key
   * @param default
   * @return
   */
  def getString(key: String, default: String): String = {
    val value = get(key)
    value match {
      case s: String => s
      case _ => default
    }
  }

  /**
   * @param key
   * @param value
   */
  def putInt(key: String, value: Int) {
    put(key, value.toString)
  }

  /**
   * @param key
   * @param def
   * @return
   */
  def getInt(key: String, default: Int): Int = {
    try {
      get(key) match { 
        case s: String => s.toInt
        case _ => default
      }
    } catch {
      case e: NumberFormatException => default
    }
  }

  /**
   * @param key
   * @param value
   */
  def putLong(key: String, value: Long) {
    put(key, value.toString)
  }

  /**
   * @param key
   * @param def
   * @return
   */
  def getLong(key: String, default: Long): Long = {
    try {
      get(key) match { 
        case s: String => s.toLong
        case _ => default
      }
    } catch {
      case e: NumberFormatException => default
    }
  }

  /**
   * @param key
   * @param value
   */
  def putBoolean(key: String, value: Boolean) {
    put(key, value.toString)
  }

  /**
   * @param key
   * @param def
   * @return
   */
  def getBoolean(key: String, default: Boolean): Boolean = {
    get(key) match { 
      case s: String if s.equalsIgnoreCase("true") => true
      case s: String if s.equalsIgnoreCase("false") => false
      case _ => default
    }
  }

  /**
   * @param key
   * @param value
   */
  def putFloat(key: String, value: Float) {
    put(key, value.toString)
  }

  /**
   * @param key
   * @param def
   * @return
   */
  def getFloat(key: String, default: Float): Float = {
    try {
      get(key) match { 
        case s: String => s.toFloat
        case _ => default
      }
    } catch {
      case e: NumberFormatException => default
    }
  }

  /**
   * @param key
   * @param value
   */
  def putDouble(key: String, value: Double) {
    put(key, value.toString)
  }

  /**
   * @param key
   * @param def
   * @return
   */
  def getDouble(key: String, default: Double): Double = {
    try {
      get(key) match { 
        case s: String => s.toDouble
        case _ => default
      }
    } catch {
      case e: NumberFormatException => default
    }
  }

  /**
   * @param key
   * @param value
   */
  def putPathList(key: String, value: java.util.List[String]) {
    if (value.length > 0) {
      put(key, value.mkString(File.pathSeparator))
    } else {
      put(key, "")
    }
  }

  def getPathList(key: String, default: java.util.List[String]): java.util.List[String] = {
    val value = get(key)
    value match {
      case s: String => s.split(File.pathSeparator).toList
      case _ => default
    }
  }

}
