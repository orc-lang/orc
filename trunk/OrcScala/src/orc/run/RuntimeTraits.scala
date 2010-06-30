package orc.run

import orc.values.sites.Site
import orc.values.Value
import orc.error.OrcException
import scala.concurrent.SyncVar


trait DirectInvocation extends Orc {
  def invoke(t: this.Token, s: Site, vs: List[Value]) { s.call(vs,t) }
}

trait PublishToConsole extends Orc {
  override def emit(v: Value) { print("Published: " + v + "   = " + v.toOrcSyntax() + "\n") }
}


trait ActorScheduler extends Orc {
  val done: SyncVar[Unit] = new SyncVar()
  
  def waitUntilFinished { done.get }
  
  def halted { worker ! None }
  
  val worker = new Worker()
  
  import scala.actors.Actor
  import scala.actors.Actor._

  worker.start
  
  override def schedule(ts: List[Token]) { for (t <- ts) worker ! Some(t) }
  class Worker extends Actor {
    def act() {
      loop {
        react {
          case Some(x:Token) => x.run
          
          // execution has halted
          case None => {
            timer.cancel()
            done.set({})
            exit 
          }
        }
      }
    }
  }
}


trait StandardOrcExecution extends Orc 
with DirectInvocation 
with ActorScheduler
{
  def emit(v : Value) = { /* By default, ignore toplevel publications */ }
  def expressionPrinted(s: String) { print(s) }
  def caught(e: Throwable) { 
    e match {
      case (ex: OrcException) => println(ex.getPosition().longString) 
    }
    e.printStackTrace() 
  }
}