//
// SupportForClasses.scala -- Scala trait SupportForClasses
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.extensions

import orc.run.Orc
import orc.ast.oil.nameless.{ Expression, Call, Constant }
import orc.{ OrcExecutionOptions, OrcEvent, Handle }
import orc.error.runtime.ExecutionException
import orc.run.core.Subgroup
import orc.run.core.Execution
import orc.run.core.Group
import orc.run.core.Closure
import orc.run.core.Token
import orc.run.core.ResilientGroup

/**
  * @author dkitchin
  */
case class ResilientKilledEvent(g: ResilientGroup) extends OrcEvent

trait SupportForClasses extends Orc {
  override def installHandlers(host: Execution) {
    val thisHandler = {
      case ResilientKilledEvent(g) => {
        host.add(g)
      }
    }: PartialFunction[OrcEvent, Unit]

    host.installHandler(thisHandler)
    super.installHandlers(host)
  }
}
