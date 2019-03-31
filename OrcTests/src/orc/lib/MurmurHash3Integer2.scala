//
// MurmurHash3Integer2.scala -- Scala object MurmurHash3Integer2
// Project OrcTests
//
// Created by jthywiss on Mar 31, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib

import scala.util.hashing.MurmurHash3

import orc.types.{ IntegerType, SimpleFunctionType }
import orc.values.sites.{ FunctionalSite, TotalSite2Simple, TypedSite }

/** Hashes two Ints into an Int using MurmurHash3
  *
  * @author jthywiss
  */
object MurmurHash3Integer2 extends TotalSite2Simple[Number, Number]() with FunctionalSite with TypedSite {

  def eval(arg0: Number, arg1: Number): Any = MurmurHash3.finalizeHash(MurmurHash3.mix(MurmurHash3.mix(629068548, arg0.intValue), arg1.intValue), 2)

  override def orcType = SimpleFunctionType(IntegerType, IntegerType, IntegerType)

}
