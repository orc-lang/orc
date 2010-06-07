//
// OrcOptions.scala -- Scala trait OrcOptions
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

package orc

import scala.collection.JavaConversions._

/**
 * 
 *
 * @author jthywiss
 */
trait OrcOptions {
  def filename: String
  def filename_=(newVal: String)
  def debugLevel: Int
  def debugLevel_=(newVal: Int)
  def shortErrors: Boolean
  def shortErrors_=(newVal: Boolean)

  // Compile options
  def usePrelude: Boolean
  def usePrelude_=(newVal: Boolean)
  def includePath: java.util.List[String]
  def includePath_=(newVal: java.util.List[String])
  def additionalIncludes: java.util.List[String]
  def additionalIncludes_=(newVal: java.util.List[String])
  def exceptionsOn: Boolean
  def exceptionsOn_=(newVal: Boolean)
  def typecheck: Boolean
  def typecheck_=(newVal: Boolean)
  def quietChecking: Boolean
  def quietChecking_=(newVal: Boolean)

  // Execution options
  def maxPublications: Int
  def maxPublications_=(newVal: Int)
  def tokenPoolSize: Int
  def tokenPoolSize_=(newVal: Int)
  def stackSize: Int
  def stackSize_=(newVal: Int)
  def classPath: java.util.List[String]
  def classPath_=(newVal: java.util.List[String])
  def hasCapability(capName: String): Boolean
  def setCapability(capName: String, newVal: Boolean)
}
