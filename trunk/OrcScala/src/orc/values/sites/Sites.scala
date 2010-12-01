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
import orc.ast.oil.nameless.Type
import orc.ast.oil.nameless.Bot
import orc.TokenAPI
import orc.error.OrcException
import orc.error.NotYetImplementedException
import orc.error.runtime.ArityMismatchException
import orc.run.Logger

trait SiteMetaData {
  /* This function returns the virtual time taken by this
   * site. By default the virtual time taken by any site 
   * is considered to be 0. 
  **/ 
  def virtualTime() : Int = 0   // -1 represents infinity
}

trait Site extends OrcValue with SiteMetaData {
  def call(args: List[AnyRef], callingToken: TokenAPI): Unit
  
  def name: String = this.getClass().toString()
  def populateMetaData(args: List[AnyRef], callingToken: TokenAPI) : Unit = {}
  override def toOrcSyntax() = this.name
}

trait TypedSite extends Site {
  def orcType(): orc.types.Type
}

trait UntypedSite extends Site

trait PartialSite extends Site {
  def call(args: List[AnyRef], token: TokenAPI) {
    Logger.entering(this.getClass.getCanonicalName, "call", args)
    evaluate(args) match {
      case Some(v) => token.publish(v)
      case None => token.halt
    }
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef]
}

trait TotalSite extends Site {
  def call(args: List[AnyRef], token: TokenAPI) {
    Logger.entering(this.getClass.getCanonicalName, "call", args)
  	try { 
  	  token.publish(evaluate(args)) 
  	} catch { 
  	  case (e : OrcException) => token !! e 
  	}
  }
  
  def evaluate(args: List[AnyRef]): AnyRef
}

trait UnimplementedSite extends Site {
  override def name = "(unimplemented)"
  def orcType(argTypes: List[Type]): Nothing = {
	  throw new NotYetImplementedException("Site " + this + " is unimplemented.")
  }
  def call(args: List[AnyRef], token: TokenAPI): Nothing = {
	  throw new NotYetImplementedException("Site " + this + " is unimplemented.")
  }
}


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