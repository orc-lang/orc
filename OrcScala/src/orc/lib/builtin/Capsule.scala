package orc.lib.builtin

import orc.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.run.StandardOrcExecution
import scala.actors.Actor
import orc.TokenAPI
import orc.oil.nameless.Expression
import orc.oil.nameless.Call
import orc.oil.nameless.Constant
import orc.run.SupportForCapsules
import orc.error.OrcException

// Site site

object SiteSite extends TotalSite with UntypedSite {
  override def name = "Site"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(clo : Closure) => new Capsule(clo)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Closure", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

// Standalone capsule execution

class Capsule(clo: Closure) extends UntypedSite {
  
  override def name = "capsule " + Format.formatValue(clo)
   
  def call(args: List[AnyRef], caller: TokenAPI) {
    val node = Call(Constant(clo), args map Constant, Some(Nil))
    caller.runtime match {
      case r : SupportForCapsules => r.runEncapsulated(node, caller.asInstanceOf[r.Token])
      case _ => caller !! new OrcException("This runtime does not support encapsulated execution.")
    }
  }
  
}
