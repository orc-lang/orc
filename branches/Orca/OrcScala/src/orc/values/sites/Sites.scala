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
  def orcType(argTypes: List[Type]): Type
  
  def populateMetaData(args: List[AnyRef], callingToken: TokenAPI) : Unit = {}
  override def toOrcSyntax() = this.name
}

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

trait UntypedSite extends Site {
  def orcType(argTypes: List[Type]): Type = Bot()
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