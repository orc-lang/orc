//
// ExecutionMashaler.scala -- Scala trait ExecutionMashaler, and class ClosureReplacement
// Project OrcScala
//
// Created by jthywiss on Jan 24, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.token

import java.io.ObjectStreamException

import orc.run.core.{ Closure, ClosureGroup }
import orc.run.distrib.common.ExecutionMarshaling

/** A DOrcExecution mix-in to marshal and unmarshal dOrc execution-internal
  * objects, such as tokens, groups, closures, counters, terminators, etc.
  *
  * Note that this is called during serialization, not during
  * ValueMarshaler.marshalValue/unmarshalValue calls.
  *
  * @author jthywiss
  */
trait ExecutionMashaler extends ExecutionMarshaling[PeerLocation] {
  execution: DOrcExecution =>

  // TODO: Move TokenMashaling et al. to this trait.

  override val marshalExecutionObject: PartialFunction[(PeerLocation, AnyRef), AnyRef] = {
    case (destination, c: Closure) => {
      ClosureReplacement(c)
    }
    case (destination, cg: ClosureGroup) => {
      TokenFieldMarshaling.marshalClosureGroup(cg, execution, destination)
    }
  }

  override val unmarshalExecutionObject: PartialFunction[(PeerLocation, AnyRef), AnyRef] = {
    /* ClosureReplacement: see ClosureReplacement.readResolve() */
    case (origin, cgr: ClosureGroupReplacement) => {
      cgr.unmarshalClosureGroup(execution, origin)
    }
  }
}

/** Replacement for a Closure for use in serialization.
  *
  * @author jthywiss
  */
protected final case class ClosureReplacement(index: Int, closureGroup: ClosureGroup) {
  @throws(classOf[ObjectStreamException])
  protected def readResolve(): AnyRef = new Closure(index, closureGroup)
}

protected object ClosureReplacement {
  def apply(c: Closure) = new ClosureReplacement(c.index, c.closureGroup)
}
