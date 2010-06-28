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
      case List(clo : Closure) => new Capsule(clo)
      case List(a) => throw new ArgumentTypeMismatchException(0, "Closure", a.getClass().toString())
      case _ => throw new ArityMismatchException(1, args.size)
  }
}

// Standalone capsule execution

class Capsule(clo: Closure) extends UntypedSite with StandardOrcExecution {
  
  override def name = "_capsule_"
    
  def call(args: List[Value], caller: TokenAPI) {
    val exec = new CapsuleExecution(caller)
    val node = Call(Constant(clo), args map Constant, Some(Nil))
    val t = new Token(node, exec)
    t.run
  }
  
  class CapsuleExecution(caller: TokenAPI) extends Execution {
    
    var listener: Option[TokenAPI] = Some(caller)
    
    override def publish(t: Capsule.this.Token, v: Value) { 
      listener match {
        case Some(caller) => {
          listener = None
          t.halt
          caller.publish(v)
        }
        case None => { 
          t.halt 
        }
      }
    }
    
    override def onHalt {
      listener match {
        case Some(caller) => {
          listener = None
          caller.halt
        }
        case None => {  }
      }
    }
    
  }
  
  def emit(v: Value) { /* Do nothing. This will never be called. */ }
  
}
