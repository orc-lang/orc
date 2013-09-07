//
// SemaphoreType.scala -- Scala object SemaphoreType
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Dec 3, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.state.types

import orc.types._

/**
  * @author dkitchin
  */
object SemaphoreType extends RecordType(
  "acquire" -> SimpleFunctionType(SignalType),
  "acquireD" -> SimpleFunctionType(SignalType),
  "release" -> SimpleFunctionType(SignalType),
  "snoop" -> SimpleFunctionType(SignalType),
  "snoopD" -> SimpleFunctionType(SignalType)) {

  override def toString = "Semaphore"

  def getBuilder: Type = SimpleFunctionType(IntegerType, this)

}
