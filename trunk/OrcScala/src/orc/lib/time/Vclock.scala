//
// Vclock.scala -- Scala class Vclock
// Project OrcScala
//
// $Id: Vclock.scala 2329 2011-01-14 20:55:15Z dkitchin $
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

import orc.Handle
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
  def call(args: List[AnyRef], token: Handle) {
    token.publish(BigInt(0))//FIXME
  }

  def orcType = SimpleFunctionType(IntegerType)
}
