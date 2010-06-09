//
// SiteAdaptor.scala -- Scala class ClassAdaptor
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Jun 2, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites.compatibility

import orc.values.sites.Site
import orc.values.sites.UntypedSite
import orc.TokenAPI
import orc.values.Value
import orc.values.Literal
import orc.values.Signal
import orc.error.runtime.TokenException
import orc.types.Type

/**
 * 
 *
 * @author jthywiss
 */
abstract class SiteAdaptor extends Site with UntypedSite {

  def call(args: List[Value], token: TokenAPI) {
    val jl = new java.util.ArrayList[Object](args.size)
    for (arg <- args) arg match {
      case Literal(i: scala.math.BigInt) => jl.add(i.bigInteger)
      case Literal(d: scala.math.BigDecimal) => jl.add(d.bigDecimal)
      case Literal(v) => jl.add(v.asInstanceOf[AnyRef])
      case _ => jl.add(arg)
    }
    callSite(new Args(jl), token)
  }

  override def name: String = this.getClass().getName()

  /**
   * Must be implemented by subclasses to implement the site behavior
   * @param args          list of argument values
   * @param caller    where the result should be sent
   */
  @throws(classOf[TokenException]) def callSite(args: Args, caller: TokenAPI): Unit
  
  def object2value(o: java.lang.Object): Value = o match {
    case s: Site => s
    case i: java.math.BigInteger => new Literal(new scala.math.BigInt(i))
    case d: java.math.BigDecimal=> new Literal(new scala.math.BigDecimal(d))
    case _ => new Literal(o)
  }
  
  def signal() = Signal
}
