package orc.lib.util

import orc.values.sites.InvokerMethod
import orc.values.sites.SiteMetadata
import orc.OrcRuntime
import orc.Invoker
import java.math.BigInteger
import orc.OnlyDirectInvoker
import java.util.concurrent.ThreadLocalRandom
import orc.values.sites.Range
import orc.IllegalArgumentInvoker
import java.lang.IllegalArgumentException
import orc.values.sites.FunctionalSite

object Random extends InvokerMethod with SiteMetadata with FunctionalSite {
  class ArgInvoker extends OnlyDirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target == Random && arguments.length == 1 && arguments(0).isInstanceOf[Number]
    }

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      val n = arguments(0).asInstanceOf[Number]
      if (n.longValue() <= Int.MaxValue)
        ThreadLocalRandom.current().nextInt(n.intValue()).asInstanceOf[AnyRef]
      else
        throw new IllegalArgumentException(s"$Random($n): bound much be less than 2**31.")
    }
  }
  class NoArgInvoker extends OnlyDirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target == Random && arguments.length == 0
    }

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      ThreadLocalRandom.current().nextInt().asInstanceOf[AnyRef]
    }
  }
  
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    args.length match {
      case 0 => new NoArgInvoker()
      case 1 =>
        args(0) match {
          case n: BigInt => new ArgInvoker()
          case n: BigInteger => new ArgInvoker()
          case n: Integer => new ArgInvoker()
          case _ =>
            IllegalArgumentInvoker(this, args)
        }
      case _ =>
        IllegalArgumentInvoker(this, args)
    }
  }
  
  override def publications: Range = Range(0, 1)
  override def isDirectCallable: Boolean = true
}

object URandom extends InvokerMethod with SiteMetadata with FunctionalSite {
  class NoArgInvoker extends OnlyDirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target == URandom && arguments.length == 0
    }

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      ThreadLocalRandom.current().nextDouble().asInstanceOf[AnyRef]
    }
  }
  
  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    args.length match {
      case 0 => new NoArgInvoker()
      case _ =>
        IllegalArgumentInvoker(this, args)
    }
  }
  
  override def publications: Range = Range(0, 1)
  override def isDirectCallable: Boolean = true
}
