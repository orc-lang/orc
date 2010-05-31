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
package orc.sites

/**
 * Services (such as name resolution) for normal Orc sites.
 *
 * @author jthywiss
 */
object OrcSiteForm {
  def resolve(name: String) = loadClass(lookupInternalSites(name))
  private def loadClass(name:String) = getClass().getClassLoader().loadClass(name) //TODO:FIXME: This should use the OrcAPI's loadClass, and the classpath from the OrcOptions
  def lookupInternalSites(name: String): String = name match {
    case "Cons" => "orc.lib.builtin.Cons"
    case "Eq" => "orc.lib.builtin.Eq"
    case "If" => "orc.lib.builtin.If"
    case "IsCons" => "orc.lib.builtin.IsCons"
    case "Not" => "orc.lib.builtin.Not"
    case "Tuple" => "orc.lib.builtin.Tuple"
    case "Unapply" => "orc.lib.builtin.Unapply" 
    case _ => name
  }
}
