//
// OrcSiteForm.scala -- Scala object OrcSiteForm
// Project OrcScala
//
// Created by jthywiss on May 30, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import orc.error.compiletime.SiteResolutionException
import orc.compile.Logger
import orc.values.HasMembers

/** Services (such as name resolution) for normal Orc sites.
  *
  * @author jthywiss
  */
object OrcSiteForm extends SiteForm {
  @throws(classOf[SiteResolutionException])
  def resolve(name: String): AnyRef = {
    Logger.finer("Resolving Orc site " + name)
    var loadedClass: java.lang.Class[_] = null
    try {
      loadedClass = loadClass(name)
    } catch {
      case e: InterruptedException => throw e
      case e: Exception =>
        throw new SiteResolutionException(name, e)
      case e: ThreadDeath => throw e
      case e: VirtualMachineError => throw e
      case e: Error =>
        throw new SiteResolutionException(name, e)
    }
    if (classOf[Site].isAssignableFrom(loadedClass) || classOf[HasMembers].isAssignableFrom(loadedClass)) {
      try {
        return loadedClass.asInstanceOf[Class[_ <: AnyRef]].newInstance()
      } catch {
        case e: InterruptedException => throw e
        case e: Exception =>
          throw new SiteResolutionException(loadedClass.getName, e)
        case e: ThreadDeath => throw e
        case e: VirtualMachineError => throw e
        case e: Error =>
          throw new SiteResolutionException(loadedClass.getName, e)
      }
    } else {
      try { // Maybe it's a Scala object....
        val loadedClassCompanion = loadClass(name + "$")
        val instance = loadedClassCompanion.getField("MODULE$").get(null)
        loadedClass = instance.getClass()
        if(classOf[Site].isAssignableFrom(loadedClass) || classOf[HasMembers].isAssignableFrom(loadedClass)) {
          return instance
        }
      } catch {
        case e: InterruptedException => throw e
        case _: Exception => {} //Ignore -- It's not a Scala object, then.
      }
      throw new SiteResolutionException(loadedClass.getName, new ClassCastException(loadedClass.getName + " cannot be cast to " + classOf[Site].getName))
    }
  }
}
