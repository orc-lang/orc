//
// Vtimer.scala -- Scala class/trait/object Vtimer
// Project OrcScala
//
// $Id$
//
// Created by amshali on Sep 30, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.time

import orc.values.Signal
import orc.TokenAPI
import orc.values.sites.UntypedSite
import orc.values.sites.Site
/**
 * 
 *
 * @author amshali
 */
class Vtimer extends Site with UntypedSite {
  def call(args: List[AnyRef], token: TokenAPI) {
    val n : scala.math.BigInt  = args(0).asInstanceOf[scala.math.BigInt]
    if (n.toInt == 0) {
      token.publish(Signal)
    }
  }
  override def populateMetaData(args: List[AnyRef], callingToken: TokenAPI) : Unit = {
    val n : scala.math.BigInt  = args(0).asInstanceOf[scala.math.BigInt]
    vtime = n.toInt
  }

  var vtime : Int = 0
  override def virtualTime() : Int = vtime   // -1 represents infinity

  override def name: String = this.getClass().getName()
}

