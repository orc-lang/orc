package orc.run

import orc.values.sites.Site
import orc.values.Value
import scala.concurrent.SyncVar


trait DirectInvocation extends Orc {
  def invoke(t: this.Token, s: Site, vs: List[Value]) { s.call(vs,t) }
}

trait PublishToConsole extends Orc {
  def emit(v: Value) { print("Published: " + v + "   = " + v.toOrcSyntax() + "\n") }
}


trait ActorScheduler extends Orc {
  val done: SyncVar[Unit] = new SyncVar()
  
  def waitUntilFinished { done.get }
  
  def halted { worker ! None ; done.set({}) }
  
  val worker = new Worker()
  
  import scala.actors.Actor
  import scala.actors.Actor._

  worker.start
  
  override def schedule(ts: List[Token]) { for (t <- ts) worker ! Some(t) }
  class Worker extends Actor {
    def act() {
      def loop() {
        receive {
          case Some(x:Token) => x.run ; loop()
          case None => { } // execution has halted
        }
      }
      loop()
    }
  }
}


trait StandardOrcExecution extends Orc 
with DirectInvocation 
with ActorScheduler
{
  def expressionPrinted(s: String) { print(s) }
  def caught(e: Throwable) { e.printStackTrace() } 
}