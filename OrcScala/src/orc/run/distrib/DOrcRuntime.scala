//
// DOrcRuntime.scala -- Scala class DOrcRuntime
// Project OrcScala
//
// Created by jthywiss on Dec 29, 2015.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

import scala.collection.JavaConversions.collectionAsScalaIterable

import orc.run.StandardOrcRuntime
import orc.run.extensions.SupportForDOrc

/** Distributed Orc (dOrc) Runtime Engine.
  *
  * Rule of thumb: Orc Runtimes manage external interaction, with the
  * environment. Program state and engine-internal behavior is the bailiwick
  * of Orc Executions.
  *
  * @author jthywiss
  */
abstract class DOrcRuntime(engineInstanceName: String) extends StandardOrcRuntime(engineInstanceName) with SupportForDOrc {

  protected val followerNumLocationMap: java.util.concurrent.ConcurrentHashMap[Int, Location]

  def locationForFollowerNum(followerNum: Int): Location = followerNumLocationMap.get(followerNum)

  val here: Location = Here
  
  def allLocations = collectionAsScalaIterable(followerNumLocationMap.values).toSet

  object Here extends Location {
    def send(message: OrcPeerCmd) = throw new UnsupportedOperationException("Cannot send dOrc messages to self")
  }

}
