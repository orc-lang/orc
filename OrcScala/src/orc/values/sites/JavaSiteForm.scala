//
// JavaSiteForm.scala -- Scala object JavaSiteForm and class JavaClassProxy
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

import orc.TokenAPI
import orc.oil.nameless.Type // FIXME: Typechecker should operate on named types instead
import orc.values.Value

/**
 * Services (such as name resolution) for use of plain old Java classes
 * as Orc sites.
 *
 * @author jthywiss
 */
object JavaSiteForm extends SiteForm {
  def resolve(name: String) = {
    new JavaClassProxy(loadClass(name))
  }
  private def loadClass(name:String) = getClass().getClassLoader().loadClass(name) //TODO:FIXME: This should use the OrcAPI's loadClass, and the classpath from the OrcOptions
}

/**
 * Wrapper for a plain old Java class as an Orc site
 *
 * @author jthywiss
 */
class JavaClassProxy(val javaClass: Class[_]) extends Site {
  override lazy val name = javaClass.getName()
  override def orcType(argTypes: List[Type]) = null //TODO:FIXME: Implement this
  override def call(args: List[Value], token: TokenAPI) { println("Java site "+name+" called") }  //TODO:FIXME: Implement this
}
