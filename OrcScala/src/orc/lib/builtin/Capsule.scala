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

// Site site

object SiteSite extends TotalSite with UntypedSite {
  override def name = "Site"
  def evaluate(args: List[Value]) =
    args match {
      case List(clo : Closure) => {
        new Site with UntypedSite {
          override def name = "_capsule_"
          def call(args: List[Value], token: TokenAPI) {
            val node = Call(Constant(clo), args map Constant, Some(Nil))
            val capsule = new CapsuleExecution(token, node)
            capsule.start
          }
        }
      }
      case List(a) => throw new ArgumentTypeMismatchException(0, "Closure", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

class CapsuleExecution(caller: TokenAPI, node: Expression) extends StandardOrcExecution with Actor {
  
  var listener: Option[TokenAPI] = Some(caller)
  
  def act() {
    this.run(node)
  }
  
  def emit(v: Value) { 
    listener foreach { 
      listener = None
      _.publish(v)
    }
  }
  
  override def halted {
    listener foreach {
      listener = None
      _.halt
    }
    super.halted
  }
}