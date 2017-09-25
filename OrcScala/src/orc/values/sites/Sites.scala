//
// Sites.scala -- Scala traits Site, PatialSite, and UntypedSite
// Project OrcScala
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.CallContext
import orc.error.{ NotYetImplementedException, OrcException }
import orc.error.compiletime.typing.TypeException
import orc.error.runtime.{ ArityMismatchException, ExceptionHaltException, HaltException, RightException }
import orc.run.Logger
import orc.types.{ Bot, RecordType, Type }
import orc.util.ArrayExtensions.{ Array0, Array1, Array2, Array3 }
import orc.values.{ OrcRecord, OrcValue }

//FIXME:XXX: "Serializable" here is a temporary hack.  Sites are not all Serializable.
trait Site extends OrcValue with SiteMetadata with Serializable {
  def call(args: Array[AnyRef], callContext: CallContext): Unit

  override def toOrcSyntax() = this.name

  def requireRight(callContext: CallContext, rightName: String) {
    if (!callContext.hasRight(rightName)) {
      throw new RightException(rightName);
    }
  }
}

trait DirectSite extends Site {
  override val isDirectCallable = true

  def call(args: Array[AnyRef], callContext: CallContext): Unit // This could be implemented here if it was useful

  def calldirect(args: Array[AnyRef]): AnyRef
}

/* A site which provides type information. */
trait TypedSite extends Site {
  @throws(classOf[TypeException])
  def orcType(): Type
}

/* A site which explicitly lacks type information. */
/* Use sparingly; this is equivalent to using a type override */
trait UntypedSite extends TypedSite {
  def orcType() = Bot
}

trait SpecificArity extends Site {
  val arity: Int
}

// TODO: TotalSite and PartialSite will not work correctly if they are DirectSites and any site is actually blocking.

/* Enforce totality */
trait TotalSite extends DirectSite with EffectFreeAfterPubSite {
  def call(args: Array[AnyRef], callContext: CallContext) {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
    try {
      callContext.publish(evaluate(args))
    } catch {
      case (e: OrcException) => callContext.halt(e)
    }
  }
  def calldirect(args: Array[AnyRef]): AnyRef = {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
    try {
      evaluate(args)
    } catch {
      case e: Exception =>
        //throw HaltException.SINGLETON
        throw new ExceptionHaltException(e)
    }
  }

  def evaluate(args: Array[AnyRef]): AnyRef

  override def publications: Range = super.publications intersect Range(0, 1)
}

/* Enforce nonblocking, but do not enforce totality */
trait PartialSite extends DirectSite with EffectFreeAfterPubSite {
  def call(args: Array[AnyRef], callContext: CallContext) {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
    evaluate(args) match {
      case Some(v) => callContext.publish(v)
      case None => callContext.halt
    }
  }
  def calldirect(args: Array[AnyRef]): AnyRef = {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
    (try {
      evaluate(args)
    } catch {
      case e: Exception =>
        //throw HaltException.SINGLETON
        throw new ExceptionHaltException(e)
    }) match {
      case Some(v) => v
      case None => throw HaltException.SINGLETON
    }
  }

  def evaluate(args: Array[AnyRef]): Option[AnyRef]

  override def publications: Range = super.publications intersect Range(0, 1)
}

trait UnimplementedSite extends Site {
  override def name = "(unimplemented)"
  def orcType(argTypes: List[Type]): Nothing = {
    throw new NotYetImplementedException("Site " + this + " is unimplemented.")
  }
  def call(args: Array[AnyRef], callContext: CallContext): Nothing = {
    throw new NotYetImplementedException("Site " + this + " is unimplemented.")
  }
}

/* Enforce arity only */
trait Site0 extends Site with SpecificArity {

  val arity = 0

  def call(args: Array[AnyRef], callContext: CallContext) {
    args match {
      case Array0() => call(callContext)
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

  def call(callContext: CallContext): Unit

}

trait Site1 extends Site with SpecificArity {

  val arity = 1

  def call(args: Array[AnyRef], callContext: CallContext) {
    args match {
      case Array1(a) => call(a, callContext)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

  def call(a: AnyRef, callContext: CallContext): Unit

}

trait Site2 extends Site with SpecificArity {

  val arity = 2

  def call(args: Array[AnyRef], callContext: CallContext) {
    args match {
      case Array2(a, b) => call(a, b, callContext)
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }

  def call(a: AnyRef, b: AnyRef, callContext: CallContext): Unit

}

/* Enforce arity and nonblocking, but do not enforce totality */
trait PartialSite0 extends PartialSite with SpecificArity {

  val arity = 0

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    args match {
      case Array0() => eval()
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

  def eval(): Option[AnyRef]
}

trait PartialSite1 extends PartialSite with SpecificArity {

  val arity = 1

  def evaluate(args: Array[AnyRef]): Option[AnyRef] = {
    args match {
      case Array1(x) => eval(x)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

  def eval(x: AnyRef): Option[AnyRef]
}

trait PartialSite2 extends PartialSite with SpecificArity {

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
  "unapply" -> unapplySite) with TypedSite {

  // If we are called, call apply. This is needed since .apply passthrough only works on things that are not already callable.
  def call(args: Array[AnyRef], callContext: CallContext) = applySite.call(args, callContext)

  def orcType() = new RecordType(
    "apply" -> applySite.orcType(),
    "unapply" -> unapplySite.orcType())
}

trait NonBlockingSite extends SiteMetadata {
  override def timeToPublish: Delay = Delay.NonBlocking
  override def timeToHalt: Delay = Delay.NonBlocking
}

trait EffectFreeSite extends SiteMetadata {
  override def effects: Effects = Effects.None
}
trait EffectFreeAfterPubSite extends SiteMetadata {
  override def effects: Effects = Effects.BeforePub
}

trait TalkativeSite extends SiteMetadata {
  override def publications: Range = super.publications intersect Range(1, None)
}

trait FunctionalSite extends SiteMetadata with NonBlockingSite with EffectFreeSite
