package orc.lib.builtin

import orc.ast.oil.nameless.Type
import orc.values._
import orc.values.sites._
import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.RuntimeSupportException
import orc.TokenAPI
import orc.ast.oil.nameless.Expression
import orc.ast.oil.nameless.Call
import orc.ast.oil.nameless.Constant
import orc.run.extensions.SupportForclasss
import orc.error.OrcException

// Site site

object SiteSite extends TotalSite with UntypedSite {
  override def name = "MakeSite"
  def evaluate(args: List[AnyRef]) =
    args match {
      case List(closure) => new Capsule(closure)
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

// Standalone class execution

class Capsule(closure: AnyRef) extends UntypedSite {
  
  override def name = "class " + Format.formatValue(closure)
   
  def call(args: List[AnyRef], caller: TokenAPI) {
    val node = Call(Constant(closure), args map Constant, Some(Nil))
    caller.runtime match {
      case r : SupportForclasss => r.runEncapsulated(node, caller.asInstanceOf[r.Token])
      case _ => caller !! new RuntimeSupportException("encapsulated execution")
    }
  }
  
}
