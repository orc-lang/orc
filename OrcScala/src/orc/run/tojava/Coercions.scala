//
// Coercions.scala -- Scala object Coercions
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.tojava

import orc.values.sites.DirectSite
import orc.run.Logger
import orc.values.sites.Site
import orc.values.HasMembers
import orc.values.OrcObjectBase
import orc.run.core.BoundValue
import orc.run.core.BoundStop
import orc.run.core.BoundReadable
import orc.values.Field
import scala.collection.JavaConverters._

/** @author amp
  */
object Coercions {
  val applyField = new Field("apply")

  /** Coerce any value to a Callable.
    *
    * If it is a Callable, just use that. Otherwise assume it is a Java object
    * we would like to call and wrap it for a runtime based invocation.
    */
  def coerceSiteToCallable(v: AnyRef): Callable = {
    v match {
      case c: Callable => c
      // TODO: We may want to optimize cases like records and site calls.
      //case s: Site => new SiteCallable(s)
      case v => new RuntimeCallable(v)
    }
  }

  def coerceSiteToDirectCallable(v: AnyRef): DirectCallable = {
    v match {
      case c: DirectCallable => c
      case f: Future => throw new IllegalArgumentException(s"Cannot direct call a future: $f")
      case v: DirectSite => new RuntimeDirectCallable(v)
      case s: Site => throw new IllegalArgumentException(s"Non-direct site found when direct site expected.")
    }
  }

  def isInstanceOfDef(v: AnyRef): Boolean = {
    // This is kind of opaque, but all defs are always forcable and no other callable is.
    v.isInstanceOf[ForcableCallableBase]
  }

  //def coerceToContinuation(v: AnyRef): Continuation = v.asInstanceOf[Continuation]
  //def coerceToTerminator(v: AnyRef): Terminator = v.asInstanceOf[Terminator]
  //def coerceToCounter(v: AnyRef): Counter = v.asInstanceOf[Counter]
}
