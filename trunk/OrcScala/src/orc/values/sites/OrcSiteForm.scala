//
// OrcSiteForm.scala -- Scala class/trait/object OrcSiteForm
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on May 30, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.error.compiletime.SiteResolutionException
import orc.compile.Logger

/**
 * Services (such as name resolution) for normal Orc sites.
 *
 * @author jthywiss
 */
object OrcSiteForm extends SiteForm {
  @throws(classOf[ClassNotFoundException])
  def resolve(name: String): Site = {
    Logger.finer("Resolving Orc site "+name)
    val loadedClass = loadClass(name)
    if (classOf[Site].isAssignableFrom(loadedClass)) {
      try {
        return loadedClass.asInstanceOf[Class[Site]].newInstance()
      } catch {
        case e =>
          throw new SiteResolutionException(loadedClass.getName(), e)
      }
    } else {
      try { // Maybe it's a Scala object....
        val loadedClassCompanion = loadClass(name+"$")
        return loadedClassCompanion.getField("MODULE$").get(null).asInstanceOf[Site]
      } catch {
        case _ => { } //Ignore -- It's not a Scala object, then.
      }
      throw new SiteResolutionException(loadedClass.getName(),new ClassCastException(loadedClass.getClass().getCanonicalName()+" cannot be cast to "+classOf[Site].getCanonicalName()))
    }
  }
}
