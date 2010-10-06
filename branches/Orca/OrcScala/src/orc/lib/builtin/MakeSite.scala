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

import orc.TokenAPI
import orc.ast.oil.nameless.{Constant, Call}
import orc.error.runtime.{RuntimeSupportException, ArityMismatchException}
import orc.run.extensions.SupportForClasses
import orc.values._
import orc.values.sites._

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
   
  def call(args: List[AnyRef], caller: TokenAPI) {
    val node = Call(Constant(closure), args map Constant, Some(Nil))
    caller.runtime match {
      case r: SupportForClasses => r.runEncapsulated(node, caller.asInstanceOf[r.Token])
      case _ => caller !! new RuntimeSupportException("encapsulated execution")
    }
  }
  
}
