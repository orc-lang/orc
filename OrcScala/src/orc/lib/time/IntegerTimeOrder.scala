//
// IntegerTimeOrder.scala -- Scala object IntegerTimeOrder
// Project OrcScala
//
// Created by dkitchin on Aug 9, 2011.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.time

import orc.types.{ FunctionType, IntegerType, StrictCallableType }
import orc.values.sites.{ TotalSite2Simple, LocalSingletonSite, TypedSite }

/** For use with Vtime, this is the time scale using integer points.
  *
  * @author dkitchin
  */
object IntegerTimeOrder extends TotalSite2Simple[Number, Number] with TypedSite with Serializable with LocalSingletonSite {

  def eval(x: Number, y: Number) = {
    x.longValue compare y.longValue
  }

  lazy val orcType = new FunctionType(Nil, List(IntegerType, IntegerType), IntegerType) with StrictCallableType
}
