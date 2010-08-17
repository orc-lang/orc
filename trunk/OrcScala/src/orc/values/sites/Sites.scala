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

trait Site extends OrcValue {
  def call(args: List[AnyRef], callingToken: TokenAPI): Unit
  def name: String = this.getClass().toString()
  def orcType(argTypes: List[Type]): Type
  override def toOrcSyntax() = this.name
}

trait PartialSite extends Site {
  def call(args: List[AnyRef], token: TokenAPI) {
    evaluate(args) match {
      case Some(v) => token.publish(v)
      case None => token.halt
    }
  }

  def evaluate(args: List[AnyRef]): Option[AnyRef]
}

trait TotalSite extends Site {
  def call(args: List[AnyRef], token: TokenAPI) {
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