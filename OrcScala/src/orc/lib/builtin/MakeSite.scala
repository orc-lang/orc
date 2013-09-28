//
// MakeSite.scala -- Scala classes MakeSite and RunLikeSite
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.builtin

import orc.Handle
import orc.error.runtime.ArityMismatchException
import orc.error.compiletime.typing.ArgumentTypecheckingException
import orc.error.compiletime.typing.ExpectedType
import orc.values.sites.TotalSite1
import orc.values.sites.TypedSite
import orc.values.sites.UntypedSite
import orc.values.Format
import orc.types.UnaryCallableType
import orc.types.FunctionType
import orc.types.Type
//import orc.run.extensions.InstanceEvent
import orc.error.runtime.ArgumentTypeMismatchException
import orc.run.core.Closure

// MakeSite site
/*
object MakeSite extends TotalSite1 with TypedSite {
  override def name = "MakeSite"
  def eval(arg: AnyRef) = {
    arg match {
      case c: Closure => new RunLikeSite(c)
      case _ => throw new ArgumentTypeMismatchException(0, "Closure", arg.getClass().getCanonicalName())
    }
  }

  def orcType() = new UnaryCallableType {
    def call(argType: Type): Type = {
      argType match {
        case f: FunctionType => f
        case g => throw new ArgumentTypecheckingException(0, ExpectedType("a function type"), g)
      }
    }
  }

  override val effectFree = true
}
*/

class UnimplementedMakeSitePart(override val name: String) extends TotalSite1 with TypedSite {
  def eval(arg: AnyRef) = {
    arg match {
      case c: Closure => throw new Error(s"$name should never actually be called. The compiler should remove all calls.")
      case _ => throw new ArgumentTypeMismatchException(0, "Closure", arg.getClass().getCanonicalName())
    }
  }

  def orcType() = new UnaryCallableType {
    def call(argType: Type): Type = {
      argType match {
        case f: FunctionType => f
        case g => throw new ArgumentTypecheckingException(0, ExpectedType("a function type"), g)
      }
    }
  }

  override val effectFree = true
}

sealed class MakeResilient(val n: Int) extends UnimplementedMakeSitePart(s"MakeResilient$n")

class MakeResilient0 extends MakeResilient(0)
class MakeResilient1 extends MakeResilient(1)
class MakeResilient2 extends MakeResilient(2)
class MakeResilient3 extends MakeResilient(3)
class MakeResilient4 extends MakeResilient(4)
class MakeResilient5 extends MakeResilient(5)
class MakeResilient6 extends MakeResilient(6)
class MakeResilient7 extends MakeResilient(7)
class MakeResilient8 extends MakeResilient(8)
class MakeResilient9 extends MakeResilient(9)
class MakeResilient10 extends MakeResilient(10)
class MakeResilient11 extends MakeResilient(11)
class MakeResilient12 extends MakeResilient(12)
class MakeResilient13 extends MakeResilient(13)
class MakeResilient14 extends MakeResilient(14)
class MakeResilient15 extends MakeResilient(15)
class MakeResilient16 extends MakeResilient(16)
class MakeResilient17 extends MakeResilient(17)
class MakeResilient18 extends MakeResilient(18)
class MakeResilient19 extends MakeResilient(19)
class MakeResilient20 extends MakeResilient(20)
class MakeResilient21 extends MakeResilient(21)
class MakeResilient22 extends MakeResilient(22)

/*
object MakeResilients {
  val sites = 0 to 22 map (new MakeResilient(_))
}
*/

/*
// Standalone class execution

class RunLikeSite(closure: Closure) extends UntypedSite {

  override def name = "class " + Format.formatValue(closure)

  def call(args: List[AnyRef], caller: Handle) {
    caller.notifyOrc(InstanceEvent(closure, args, caller))
  }

}

*/