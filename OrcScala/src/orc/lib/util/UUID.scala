//
// UUID.scala -- Scala site UUID
// Project OrcScala
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util

import orc.types.{ SimpleFunctionType, StringType, Type }
import orc.values.sites.{ TotalSite0Simple, TypedSite }

/**
  * Generate random UUIDs.
  *
  * @author quark
  */
object UUID extends TotalSite0Simple with TypedSite {

  override def eval() =
    java.util.UUID.randomUUID().toString

  override def orcType(): Type = SimpleFunctionType(StringType)

}
