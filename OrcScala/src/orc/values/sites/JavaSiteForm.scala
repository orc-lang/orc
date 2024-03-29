//
// JavaSiteForm.scala -- Scala object JavaSiteForm
// Project OrcScala
//
// Created by jthywiss on May 30, 2010.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.error.compiletime.SiteResolutionException

/** Services (such as name resolution) for use of plain old Java classes
  * as Orc sites.
  *
  * @author jthywiss
  */
object JavaSiteForm extends SiteForm {
  @throws(classOf[SiteResolutionException])
  def resolve(name: String): Class[_] = {
    Logger.finer("Resolving Java class " + name)
    try {
      loadClass(name)
    } catch {
      case e: InterruptedException => throw e
      case e: Exception =>
        throw new SiteResolutionException(name, e)
    }
  }
}
