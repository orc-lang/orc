//
// Sites.scala -- Scala traits Site, PatialSite, and UntypedSite
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.values.{ OrcValue, Field, OrcRecord }
import orc.Handle
import orc.error.OrcException
import orc.error.compiletime.typing.TypeException
import orc.error.NotYetImplementedException
import orc.error.runtime.ArityMismatchException
import orc.run.Logger
import orc.types.Type
import orc.types.Bot
import orc.types.RecordType
import orc.error.runtime.RightException

trait SiteMetadata {
  def name: String = Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName)
  // BIG TODO: Remove this default value
  val quiescentWhileInvoked: Boolean = false
  
  def immediateHalt: Boolean = false
  def immediatePublish: Boolean = false
  def publications: (Int, Option[Int]) = (0, None)
  def effectFree: Boolean = false
}

trait Site extends OrcValue with SiteMetadata {
  def call(args: List[AnyRef], h: Handle): Unit

  override def toOrcSyntax() = this.name

  def requireRight(h: Handle, rightName: String) {
    if (!h.hasRight(rightName)) {
      throw new RightException(rightName);
    }
  }
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

/* Enforce totality */
trait TotalSite extends Site {
  def call(args: List[AnyRef], h: Handle) {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
    try {
      h.publish(evaluate(args))
    } catch {
      case (e: OrcException) => h !! e
    }
  }

  def evaluate(args: List[AnyRef]): AnyRef
  
  override val immediateHalt = true // FIXME: Is this correct? It could block.
  //override val immediatePublish = true
  override val publications = (0, Some(1))
}

/* Enforce nonblocking, but do not enforce totality */
trait PartialSite extends Site {
  def call(args: List[AnyRef], h: Handle) {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
    evaluate(args) match {
      case Some(v) => h.publish(v)
      case None => h.halt
    }
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef]

  override val immediateHalt = true // FIXME: Is this correct? It could block.
  override val publications = (0, Some(1))
}

trait UnimplementedSite extends Site {
  override def name = "(unimplemented)"
  def orcType(argTypes: List[Type]): Nothing = {
    throw new NotYetImplementedException("Site " + this + " is unimplemented.")
  }
  def call(args: List[AnyRef], h: Handle): Nothing = {
    throw new NotYetImplementedException("Site " + this + " is unimplemented.")
  }
}

/* Enforce arity only */
trait Site0 extends Site with SpecificArity {

  val arity = 0

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case Nil => call(h)
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

  def call(h: Handle): Unit

}

trait Site1 extends Site with SpecificArity {

  val arity = 1

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(a) => call(a, h)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

  def call(a: AnyRef, h: Handle): Unit

}

trait Site2 extends Site with SpecificArity {

  val arity = 2

  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(a, b) => call(a, b, h)
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }

  def call(a: AnyRef, b: AnyRef, h: Handle): Unit

}

/* Enforce arity and nonblocking, but do not enforce totality */
trait PartialSite0 extends PartialSite with SpecificArity {

  val arity = 0

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    args match {
      case Nil => eval()
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

  def eval(): Option[AnyRef]
}

trait PartialSite1 extends PartialSite with SpecificArity {

  val arity = 1

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    args match {
      case List(x) => eval(x)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

  def eval(x: AnyRef): Option[AnyRef]
}

trait PartialSite2 extends PartialSite with SpecificArity {

  val arity = 2

  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    args match {
      case List(x, y) => eval(x, y)
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }

  def eval(x: AnyRef, y: AnyRef): Option[AnyRef]
}

/* Enforce arity and totality */
trait TotalSite0 extends TotalSite with SpecificArity {

  val arity = 0

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List() => eval()
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }

  def eval(): AnyRef
}

trait TotalSite1 extends TotalSite with SpecificArity {

  val arity = 1

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(x) => eval(x)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }

  def eval(x: AnyRef): AnyRef
}

trait TotalSite2 extends TotalSite with SpecificArity {

  val arity = 2

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(x, y) => eval(x, y)
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }

  def eval(x: AnyRef, y: AnyRef): AnyRef
}

trait TotalSite3 extends TotalSite with SpecificArity {

  val arity = 3

  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(x, y, z) => eval(x, y, z)
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
  def orcType() = new RecordType(
    "apply" -> applySite.orcType(),
    "unapply" -> unapplySite.orcType())
}
