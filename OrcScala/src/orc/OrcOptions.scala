//
// OrcOptions.scala -- Interfaces for Orc options
// Project OrcScala
//
// Created by amp on July 12, 2017.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc

import java.net.InetSocketAddress
import java.nio.file.Path

import orc.compile.CompilerFlagValue

/** Options for Orc compilation and execution.
  *
  * @author jthywiss
  */
trait OrcOptions extends OrcCompilationOptions with OrcExecutionOptions

trait OrcCommonOptions extends Serializable {
  def filename: String
  def filename_=(newVal: String)
  def classPath: java.util.List[String]
  def classPath_=(newVal: java.util.List[String])
  def backend: BackendType
  def backend_=(newVal: BackendType)
}

trait OrcCompilationOptions extends OrcCommonOptions {
  def usePrelude: Boolean
  def usePrelude_=(newVal: Boolean)
  def includePath: java.util.List[String]
  def includePath_=(newVal: java.util.List[String])
  def additionalIncludes: java.util.List[String]
  def additionalIncludes_=(newVal: java.util.List[String])
  def typecheck: Boolean
  def typecheck_=(newVal: Boolean)
  def disableRecursionCheck: Boolean
  def disableRecursionCheck_=(newVal: Boolean)
  def echoOil: Boolean
  def echoOil_=(newVal: Boolean)
  def echoIR: Int
  def echoIR_=(newVal: Int)
  def oilOutputFile: Option[Path]
  def oilOutputFile_=(newVal: Option[Path])
  def compileOnly: Boolean
  def compileOnly_=(newVal: Boolean)
  def runOil: Boolean
  def runOil_=(newVal: Boolean)

  def optimizationLevel: Int
  def optimizationLevel_=(newVal: Int)
  def optimizationOptions: java.util.List[String]
  def optimizationOptions_=(v: java.util.List[String])

  def optimizationFlags: Map[String, CompilerFlagValue]
}

trait OrcExecutionOptions extends OrcCommonOptions {
  def showJavaStackTrace: Boolean
  def showJavaStackTrace_=(newVal: Boolean)
  def disableTailCallOpt: Boolean
  def disableTailCallOpt_=(newVal: Boolean)
  def stackSize: Int
  def stackSize_=(newVal: Int)
  def maxTokens: Int
  def maxTokens_=(newVal: Int)
  def maxSiteThreads: Int
  def maxSiteThreads_=(newVal: Int)
  def hasRight(rightName: String): Boolean
  def setRight(rightName: String, newVal: Boolean)

  // For distributed runtime:
  def listenSocketAddress: InetSocketAddress
  def listenSocketAddress_=(newVal: InetSocketAddress)
  def followerCount: Int
  def followerCount_=(newVal: Int)
  def listenSockAddrFile: Option[Path]
  def listenSockAddrFile_=(newVal: Option[Path])
}
