//
// Sites.scala -- Scala traits Site, PatialSite, and UntypedSite
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.values.OrcValue
import orc.Handle
import orc.error.OrcException
import orc.error.NotYetImplementedException
import orc.error.runtime.ArityMismatchException
import orc.run.Logger
import orc.types.Type
import orc.types.Bot

trait SiteMetadata {
  def name: String = Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName)

  /**
   * Virtual time taken by this site. By default, the virtual time
   * taken by any site is considered to be 0. 
   */ 
  def virtualTime(): Int = 0   // -1 represents infinity
}

trait Site extends OrcValue with SiteMetadata {
  def call(args: List[AnyRef], h: Handle): Unit
  
  override def toOrcSyntax() = this.name
}

trait TypedSite extends Site {
  def orcType(): Type
}

/* A site which explicitly lacks type information. */
/* Use sparingly; this is equivalent to setting a type assertion */
trait UntypedSite extends TypedSite {
  def orcType() = Bot
}

trait PartialSite extends Site {
  def call(args: List[AnyRef], h: Handle) {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
    evaluate(args) match {
      case Some(v) => h.publish(v)
      case None => h.halt
    }
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef]
}

trait TotalSite extends Site {
  def call(args: List[AnyRef], h: Handle) {
    Logger.entering(Option(this.getClass.getCanonicalName).getOrElse(this.getClass.getName), "call", args)
  	try { 
  	  h.publish(evaluate(args)) 
  	} catch { 
  	  case (e : OrcException) => h !! e 
  	}
  }
  
  def evaluate(args: List[AnyRef]): AnyRef
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



/* Enforce arity, but don't assume totality */
trait Site0 extends Site {
  
  def call(args: List[AnyRef], h: Handle) {
    args match {
      case Nil => call(h)
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }
  
  def call(h: Handle): Unit

}

trait Site1 extends Site {
  
  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(a) => call(a, h)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }
  
  def call(a: AnyRef, h: Handle): Unit
  
}

trait Site2 extends Site {
  
  def call(args: List[AnyRef], h: Handle) {
    args match {
      case List(a,b) => call(a,b,h)
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }
  
  def call(a: AnyRef, b: AnyRef, h: Handle): Unit
  
}



trait PartialSite1 extends PartialSite {
  
  def evaluate(args: List[AnyRef]): Option[AnyRef] = {
    args match {
      case List(x) => eval(x)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }
  
  def eval(x: AnyRef): Option[AnyRef]
}

/* Enforce arity and totality */
trait TotalSite0 extends TotalSite {
  
  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List() => eval()
      case _ => throw new ArityMismatchException(0, args.size)
    }
  }
  
  def eval(): AnyRef
}


trait TotalSite1 extends TotalSite {
  
  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(x) => eval(x)
      case _ => throw new ArityMismatchException(1, args.size)
    }
  }
  
  def eval(x: AnyRef): AnyRef
}

trait TotalSite2 extends TotalSite {
  
  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(x,y) => eval(x,y)
      case _ => throw new ArityMismatchException(2, args.size)
    }
  }
  
  def eval(x: AnyRef, y: AnyRef): AnyRef
}

trait TotalSite3 extends TotalSite {
  
  def evaluate(args: List[AnyRef]): AnyRef = {
    args match {
      case List(x,y,z) => eval(x,y,z)
      case _ => throw new ArityMismatchException(3, args.size)
    }
  }
  
  def eval(x: AnyRef, y: AnyRef, z: AnyRef): AnyRef
}
