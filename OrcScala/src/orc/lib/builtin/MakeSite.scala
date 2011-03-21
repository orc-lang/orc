//
// MakeSite.scala -- Scala classes MakeSite and RunLikeSite
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.builtin

import orc.Handle
import orc.error.runtime.ArityMismatchException
import orc.values._
import orc.values.sites._
import orc.run.extensions.InstanceEvent

// MakeSite site

object MakeSite extends TotalSite with UntypedSite {
  override def name = "MakeSite"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(closure) => new RunLikeSite(closure)
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

// Standalone class execution

class RunLikeSite(closure: AnyRef) extends UntypedSite {
  
  override def name = "class " + Format.formatValue(closure)
   
  def call(args: List[AnyRef], caller: Handle) {
    caller.notifyOrc(InstanceEvent(closure, args, caller))
  }
  
}
