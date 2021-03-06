//
// Sites.scala -- Scala traits Site, PatialSite, and UntypedSite
// Project OrcScala
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility

import orc.error.OrcException
import orc.error.runtime.{ ArityMismatchException, HaltException, RightException }
import orc.types.RecordType
import orc.util.ArrayExtensions.{ Array0, Array1, Array2, Array3 }
import orc.values.OrcRecord
import orc.values.sites.{ Range, SpecificArity, TypedSite }

trait Site extends orc.values.sites.Site {
    self =>

  def getInvoker(runtime: orc.OrcRuntime, args: Array[AnyRef]): orc.Invoker = {
    new orc.Invoker {
      def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
        target.getClass == self.getClass
      }
      def invoke(callContext: orc.VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): orc.SiteResponseSet = {
        target.asInstanceOf[Site].call(arguments, new CallContext(callContext))
        callContext.empty()
      }
    }
  }

  def call(args: Array[AnyRef], callContext: CallContext): Unit

  override def toOrcSyntax() = this.name

  def requireRight(callContext: CallContext, rightName: String) {
    if (!callContext.hasRight(rightName)) {
      throw new RightException(rightName);
    }
  }
}

trait DirectSite extends orc.values.sites.DirectSite {
    self =>

  def getInvoker(runtime: orc.OrcRuntime, args: Array[AnyRef]): orc.DirectInvoker = {
    new orc.DirectInvoker {
      def canInvoke(target: AnyRef, arguments: Array[AnyRef]): Boolean = {
        target.getClass == self.getClass
      }
      def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
        target.asInstanceOf[DirectSite].calldirect(arguments)
      }
    }
  }

  def calldirect(args: Array[AnyRef]): AnyRef
}

// TODO: TotalSite and ScalaPartialSite will not work correctly if they are DirectSites and any site is actually blocking.

/* Enforce totality */
trait TotalSite extends DirectSite with EffectFreeAfterPubSite {
  def call(args: Array[AnyRef], callContext: CallContext) {
    //Logger.entering(orc.util.GetScalaTypeName(this), "call", args)
    try {
      orc.run.StopWatches.implementation {
        callContext.publish(evaluate(args))
      }
    } catch {
      case (e: OrcException) => callContext.halt(e)
    }
  }
  def calldirect(args: Array[AnyRef]): AnyRef = {
    //Logger.entering(orc.util.GetScalaTypeName(this), "call", args)
    try {
      orc.run.StopWatches.implementation {
        evaluate(args)
      }
    } catch {
      case e: Exception =>
        throw new HaltException(e)
    }
  }

  def evaluate(args: Array[AnyRef]): AnyRef

  override def publications: Range = super.publications intersect Range(0, 1)
}

/* Enforce nonblocking, but do not enforce totality */
trait ScalaPartialSite extends DirectSite with EffectFreeAfterPubSite {
  def call(args: Array[AnyRef], callContext: CallContext) {
    //Logger.entering(orc.util.GetScalaTypeName(this), "call", args)
    orc.run.StopWatches.implementation {
      evaluate(args) match {
        case Some(v) => callContext.publish(v)
        case None => callContext.halt
      }
    }
  }

  def calldirect(args: Array[AnyRef]): AnyRef = {
    //Logger.entering(orc.util.GetScalaTypeName(this), "call", args)
    (try {
      orc.run.StopWatches.implementation {
        evaluate(args)
      }
    } catch {
      case e: Exception =>
        throw new HaltException(e)
    }) match {
      case Some(v) => v
      case None => throw new HaltException()
    }
  }

  def evaluate(args: Array[AnyRef]): Option[AnyRef]

  override def publications: Range = super.publications intersect Range(0, 1)
}

/* Enforce arity only */
trait Site0 extends Site with SpecificArity {

  val arity = 0

  def call(args: Array[AnyRef], callContext: CallContext) {
    orc.run.StopWatches.implementation {
      args match {
        case Array0() => call(callContext)
        case _ => throw new ArityMismatchException(0, args.size)
      }
    }
  }

  def call(callContext: CallContext): Unit

}

trait Site1 extends Site with SpecificArity {

  val arity = 1

  def call(args: Array[AnyRef], callContext: CallContext) {
    orc.run.StopWatches.implementation {
      args match {
        case Array1(a) => call(a, callContext)
        case _ => throw new ArityMismatchException(1, args.size)
      }
    }
  }

  def call(a: AnyRef, callContext: CallContext): Unit

}

trait Site2 extends Site with SpecificArity {

  val arity = 2

  def call(args: Array[AnyRef], callContext: CallContext) {
    orc.run.StopWatches.implementation {
      args match {
        case Array2(a, b) => call(a, b, callContext)
        case _ => throw new ArityMismatchException(2, args.size)
      }
    }
  }

  def call(a: AnyRef, b: AnyRef, callContext: CallContext): Unit

}

/* Enforce arity and nonblocking, but do not enforce totality */
trait PartialSite0 extends ScalaPartialSite with SpecificArity {

  val arity = 0

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    args match {
      case Array0() => eval()
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

  def eval(): Option[AnyRef]
}

trait PartialSite1 extends ScalaPartialSite with SpecificArity {

  val arity = 1

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    args match {
      case Array1(x) => eval(x)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

  def eval(x: AnyRef): Option[AnyRef]
}

trait PartialSite2 extends ScalaPartialSite with SpecificArity {

  val arity = 2

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    args match {
      case Array2(x, y) => eval(x, y)
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }

  def eval(x: AnyRef, y: AnyRef): Option[AnyRef]
}

/* Enforce arity and totality */
trait TotalSite0 extends TotalSite with SpecificArity {

  val arity = 0

  def evaluate(args: Array[AnyRef]): AnyRef = {
    args match {
      case Array0() => eval()
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

  def eval(): AnyRef
}

trait TotalSite1 extends TotalSite with SpecificArity {

  val arity = 1

  def evaluate(args: Array[AnyRef]): AnyRef = {
    args match {
      case Array1(x) => eval(x)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

  def eval(x: AnyRef): AnyRef
}

trait TotalSite2 extends TotalSite with SpecificArity {

  val arity = 2

  def evaluate(args: Array[AnyRef]): AnyRef = {
    args match {
      case Array2(x, y) => eval(x, y)
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }

  def eval(x: AnyRef, y: AnyRef): AnyRef
}

trait TotalSite3 extends TotalSite with SpecificArity {

  val arity = 3

  def evaluate(args: Array[AnyRef]): AnyRef = {
    args match {
      case Array3(x, y, z) => eval(x, y, z)
      case _ => throw new ArityMismatchException(3, args.size)
    }
  }

  def eval(x: AnyRef, y: AnyRef, z: AnyRef): AnyRef
}

/* Template for building values which act as constructor-extractor sites,
 * such as the Some site.
 */
class StructurePairSite(
  applySite: TotalSite with TypedSite,
  unapplySite: PartialSite1 with TypedSite) extends OrcRecord(
  "apply" -> applySite,
  "unapply" -> unapplySite) with TypedSite with Site {

  // If we are called, call apply. This is needed since .apply passthrough only works on things that are not already callable.
  def call(args: Array[AnyRef], callContext: CallContext) = applySite.call(args, callContext)

  def orcType() = new RecordType(
    "apply" -> applySite.orcType(),
    "unapply" -> unapplySite.orcType())
}

trait NonBlockingSite extends orc.values.sites.NonBlockingSite

trait EffectFreeSite extends orc.values.sites.EffectFreeSite
trait EffectFreeAfterPubSite extends orc.values.sites.EffectFreeAfterPubSite

trait TalkativeSite extends orc.values.sites.TalkativeSite

trait FunctionalSite extends orc.values.sites.FunctionalSite
