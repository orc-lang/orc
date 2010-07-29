package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.TokenAPI
import orc.oil.nameless.Expression
import orc.oil.nameless.Call
import orc.oil.nameless.Constant
import orc.run.extensions.SupportForCapsules
import orc.error.OrcException

// Site site

object SiteSite extends TotalSite with UntypedSite {
  override def name = "Site"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(closure) => new Capsule(closure)
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

// Standalone capsule execution

class Capsule(closure: AnyRef) extends UntypedSite {
  
  override def name = "capsule " + Format.formatValue(closure)
   
  def call(args: List[AnyRef], caller: TokenAPI) {
    val node = Call(Constant(closure), args map Constant, Some(Nil))
    caller.runtime match {
      case r : SupportForCapsules => r.runEncapsulated(node, caller.asInstanceOf[r.Token])
      case _ => caller !! new OrcException("This runtime does not support encapsulated execution.")
    }
  }
  
}
