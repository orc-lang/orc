package orc.compile.orctimizer

import scala.reflect.ClassTag
import orc.ast.orctimizer.named._
import orc.values.sites.SiteMetadata
import orc.values.sites.InvokerMethod
import orc.OnlyDirectInvoker
import orc.Invoker
import orc.OrcRuntime
import orc.values.sites.Effects
import orc.values.Signal

class OrcAnnotation extends InvokerMethod with SiteMetadata {
  override def publications: orc.values.sites.Range = orc.values.sites.Range(1, 1)
  override def timeToPublish: orc.values.sites.Delay = orc.values.sites.Delay.NonBlocking
  override def timeToHalt: orc.values.sites.Delay = orc.values.sites.Delay.NonBlocking
  override def effects: Effects = Effects.None
  override def isDirectCallable: Boolean = true
  
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = new OnlyDirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = true
    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = Signal
  }
}

class Sequentialize extends OrcAnnotation

object AnnotationHack {
  def inAnnotation[A <: OrcAnnotation : ClassTag](e: Expression.Z): Boolean = {
    val TargetAnnotation = implicitly[ClassTag[A]]
    e.parents exists {
      case Branch.Z(Call.Z(Constant.Z(TargetAnnotation(_)), _, _), _, _) => 
        true
      case _ =>
        false
    }
  }
}