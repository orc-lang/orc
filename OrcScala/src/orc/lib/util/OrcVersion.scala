//
// OrcVersion.scala -- Scala site OrcVersion
// Project Orchard
//
// Created by jthywiss on Jan 20, 2013.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.util

import orc.values.sites.{ TotalSite0Simple, TypedSite }
import orc.types.{ StringType, SimpleFunctionType }

/**
 * Returns a name, version, URL, and copyright string for Orchard and Orc.
 *
 * @author jthywiss
 */
object OrcVersion extends TotalSite0Simple with TypedSite {
  def eval() = {
    import orc.Main._
    orcImplName + ' ' + orcVersion + '\n' + orcURL + '\n' + orcCopyright
  }

  def orcType = SimpleFunctionType(StringType)
}
