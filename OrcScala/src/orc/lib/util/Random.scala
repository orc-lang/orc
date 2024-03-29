//
// Random.scala -- Orc methods Random and URandom
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util

import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom

import orc.{ DirectInvoker, OrcRuntime }
import orc.values.sites.{ DirectSite, FunctionalSite, IllegalArgumentInvoker, LocalSingletonSite, Range, SiteMetadata }

object Random extends DirectSite with SiteMetadata with FunctionalSite with Serializable with LocalSingletonSite {
  class ArgInvoker extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target == Random && arguments.length == 1 && arguments(0).isInstanceOf[Number]
    }

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.implementation {
        val n = arguments(0).asInstanceOf[Number]
        if (n.longValue() <= Int.MaxValue)
          ThreadLocalRandom.current().nextInt(n.intValue()).asInstanceOf[AnyRef]
        else
          throw new IllegalArgumentException(s"$Random($n): bound much be less than 2**31.")
      }
    }
  }
  class NoArgInvoker extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target == Random && arguments.length == 0
    }

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.implementation {
        ThreadLocalRandom.current().nextInt().asInstanceOf[AnyRef]
      }
    }
  }

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]) = {
    args.length match {
      case 0 => new NoArgInvoker()
      case 1 =>
        args(0) match {
          case _: BigInt | _: BigInteger | _: Integer | _: java.lang.Long =>
            new ArgInvoker()
          case _ =>
            IllegalArgumentInvoker(this, args)
        }
      case _ =>
        IllegalArgumentInvoker(this, args)
    }
  }

  override def publications: Range = Range(0, 1)
}

object URandom extends DirectSite with SiteMetadata with FunctionalSite with Serializable with LocalSingletonSite {
  class NoArgInvoker extends DirectInvoker {
    def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
      target == URandom && arguments.length == 0
    }

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      orc.run.StopWatches.implementation {
        ThreadLocalRandom.current().nextDouble().asInstanceOf[AnyRef]
      }
    }
  }

  def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]) = {
    args.length match {
      case 0 => new NoArgInvoker()
      case _ =>
        IllegalArgumentInvoker(this, args)
    }
  }

  override def publications: Range = Range(0, 1)
}

