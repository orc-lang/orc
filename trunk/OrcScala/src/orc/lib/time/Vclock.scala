//
// Vclock.scala -- Scala class/trait/object Vclock
// Project OrcScala
//
// $Id$
//
// Created by amshali on Oct 2, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import orc.TokenAPI
import orc.values.sites.TypedSite
import orc.values.sites.Site
import orc.types.SimpleFunctionType
import orc.types.IntegerType
/**
 * 
 *
 * @author amshali
 */
class Vclock extends Site with TypedSite {
  def call(args: List[AnyRef], token: TokenAPI) {
    token.publish(BigInt(0))//FIXME
  }

  override def name: String = this.getClass().getName()
  
  def orcType = SimpleFunctionType(IntegerType)
}

