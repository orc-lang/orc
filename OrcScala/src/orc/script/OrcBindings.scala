//
// OrcBindings.scala -- Scala class OrcBindings
// Project OrcScala
//
// Created by jthywiss on May 26, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script

import java.io.File
import java.net.InetSocketAddress
import java.util.Map

import orc.{ BackendType, OrcOptions }
import orc.compile.CompilerFlagValue

import javax.script.SimpleBindings

/** An extended implementation of <code>javax.script.Bindings</code>
  * with type-specific get and put methods.
  *
  * @author jthywiss
  */
class OrcBindings(m: Map[String, Object]) extends SimpleBindings(m) with OrcOptions {
  import scala.collection.JavaConverters._

  def this() = this(new java.util.HashMap[String, Object])

  def filename: String = getString("javax.script.filename", "")
  def filename_=(newVal: String) = putString("javax.script.filename", newVal)
  def logLevel: String = getString("orc.logLevel", "INFO")
  def logLevel_=(newVal: String) = putString("orc.logLevel", newVal)
  def xmlLogFile: String = getString("orc.xmlLogFile", "")
  def xmlLogFile_=(newVal: String) = putString("orc.xmlLogFile", newVal)
  def backend: BackendType = BackendType.fromString(getString("orc.backend", "Token"))
  def backend_=(newVal: BackendType) = putString("orc.backend", newVal.toString)

  // Compile options
  def usePrelude: Boolean = getBoolean("orc.usePrelude", true)
  def usePrelude_=(newVal: Boolean) = putBoolean("orc.usePrelude", newVal)
  def includePath: java.util.List[String] = getStringList("orc.includePath", List(".").asJava)
  def includePath_=(newVal: java.util.List[String]) = putStringList("orc.includePath", newVal)
  def additionalIncludes: java.util.List[String] = getStringList("orc.additionalIncludes", List().asJava)
  def additionalIncludes_=(newVal: java.util.List[String]) = putStringList("orc.additionalIncludes", newVal)
  def typecheck: Boolean = getBoolean("orc.typecheck", false)
  def typecheck_=(newVal: Boolean) = putBoolean("orc.typecheck", newVal)
  def disableRecursionCheck: Boolean = getBoolean("orc.disableRecursionCheck", false)
  def disableRecursionCheck_=(newVal: Boolean) = putBoolean("orc.disableRecursionCheck", newVal)
  def echoOil: Boolean = getBoolean("orc.echoOil", false)
  def echoOil_=(newVal: Boolean) = putBoolean("orc.echoOil", newVal)
  def echoIR: Int = getInt("orc.echoIR", 0)
  def echoIR_=(newVal: Int) = putInt("orc.echoIR", newVal)
  def oilOutputFile: Option[File] = {
    getString("orc.oilOutputFile", "") match {
      case "" => None
      case f => Some(new File(f))
    }
  }
  def oilOutputFile_=(newVal: Option[File]) = putString("orc.oilOutputFile", newVal.map(_.toString).getOrElse(""))
  def compileOnly: Boolean = getBoolean("orc.onlyCompile", false)
  def compileOnly_=(newVal: Boolean) = putBoolean("orc.onlyCompile", newVal)
  def runOil: Boolean = getBoolean("orc.runOil", false)
  def runOil_=(newVal: Boolean) = putBoolean("orc.runOil", newVal)

  def optimizationLevel: Int = getInt("orc.optimizationLevel", 0)
  def optimizationLevel_=(newVal: Int) = {
    putInt("orc.optimizationLevel", newVal)
    optimizationLevelFlagsCache = null
  }
  def optimizationOptions: java.util.List[String] = getStringList("orc.optimizationOptions", new java.util.ArrayList(), ",")
  def optimizationOptions_=(v: java.util.List[String]) = putStringList("orc.optimizationOptions", v, ",")

  def parseOptimizationOptionLine(s: String) = {
    s.split("=") match {
      case Array(name) =>
        (name, new CompilerFlagValue(name, Some("true")))
      case Array(name, value) =>
        (name, new CompilerFlagValue(name, Some(value)))
      case _ =>
        throw new IllegalArgumentException(s"Cound not parse option: $s")
    }
  }

  private var optimizationLevelFlagsCache: scala.collection.immutable.Map[String, CompilerFlagValue] = null

  def optimizationLevelFlags: scala.collection.immutable.Map[String, CompilerFlagValue] = {
    import java.io.InputStream
    import scala.io.Source

    if (optimizationLevelFlagsCache != null)
      return optimizationLevelFlagsCache

    def loadOptLevel(n: Int): InputStream = {
      val stream = classOf[orc.OrcRuntime].getResourceAsStream(s"optimizationLevel$n.opts")
      if (stream == null && n > 0)
        loadOptLevel(n - 1)
      else
        stream
    }
    val stream = loadOptLevel(optimizationLevel)
    val flags = if (stream != null) {
      try {
        val source = Source.fromInputStream(stream)
        source.getLines.map(_.trim).map(parseOptimizationOptionLine).toMap - ""
      } finally {
        stream.close()
      }
    } else {
      scala.collection.immutable.Map[String, CompilerFlagValue]()
    }
    optimizationLevelFlagsCache = flags
    flags
  }

  def optimizationFlags = {
    val m = optimizationOptions.asScala.map(parseOptimizationOptionLine).toMap
    (optimizationLevelFlags ++ m).withDefault(n => new CompilerFlagValue(n, None))
  }

  // Execution options
  def classPath: java.util.List[String] = getStringList("orc.classPath", List().asJava)
  def classPath_=(newVal: java.util.List[String]) = putStringList("orc.classPath", newVal)
  def showJavaStackTrace: Boolean = getBoolean("orc.showJavaStackTrace", false)
  def showJavaStackTrace_=(newVal: Boolean) = putBoolean("orc.showJavaStackTrace", newVal)
  def disableTailCallOpt: Boolean = getBoolean("orc.disableTailCallOpt", false)
  def disableTailCallOpt_=(newVal: Boolean) = putBoolean("orc.disableTailCallOpt", newVal)
  def stackSize: Int = getInt("orc.stackSize", -1)
  def stackSize_=(newVal: Int) = putInt("orc.stackSize", newVal)
  def maxTokens: Int = getInt("orc.maxTokens", -1)
  def maxTokens_=(newVal: Int) = putInt("orc.maxTokens", newVal)
  def maxSiteThreads: Int = getInt("orc.maxSiteThreads", -1)
  def maxSiteThreads_=(newVal: Int) = putInt("orc.maxSiteThreads", newVal)
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

  private def string2socket(s: String) = {
    val lastColon = s.lastIndexOf(":")
    new InetSocketAddress(s.substring(0, lastColon), s.substring(lastColon + 1).toInt)
  }

  def followerSockets: java.util.List[InetSocketAddress] = getStringList("orc.distrib.followerSockets", List().asJava, ",").asScala.map(string2socket(_)).asJava
  def followerSockets_=(newVal: java.util.List[InetSocketAddress]) = putStringList("orc.distrib.followerSockets", newVal.asScala.map({ isa => isa.getHostString + ":" + isa.getPort }).asJava, ",")

  /** @param key
    * @param value
    */
  def putString(key: String, value: String) {
    put(key, value.toString)
  }

  /** @param key
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

  /** @param key
    * @param value
    */
  def putInt(key: String, value: Int) {
    put(key, value.toString)
  }

  /** @param key
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

  /** @param key
    * @param value
    */
  def putLong(key: String, value: Long) {
    put(key, value.toString)
  }

  /** @param key
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

  /** @param key
    * @param value
    */
  def putBoolean(key: String, value: Boolean) {
    put(key, value.toString)
  }

  /** @param key
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

  /** @param key
    * @param value
    */
  def putFloat(key: String, value: Float) {
    put(key, value.toString)
  }

  /** @param key
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

  /** @param key
    * @param value
    */
  def putDouble(key: String, value: Double) {
    put(key, value.toString)
  }

  /** @param key
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

  /** @param key
    * @param value
    */
  def putStringList(key: String, value: java.util.List[String], separator: String = File.pathSeparator) {
    if (value.size > 0) {
      put(key, value.asScala.mkString(separator))
    } else {
      put(key, "")
    }
  }

  def getStringList(key: String, default: => java.util.List[String], separator: String = File.pathSeparator): java.util.List[String] = {
    val value = get(key)
    value match {
      case s: String if (s.length == 0) => new java.util.ArrayList[String](0)
      case s: String => new java.util.ArrayList[String](java.util.Arrays.asList(s.split(separator): _*))
      case _ => default
    }
  }

}
