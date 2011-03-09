//
// WriteJSON.scala -- Scala class/trait/object WriteJSON
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Mar 3, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.web

import orc.values.sites.{TotalSite1, UntypedSite}
import orc.error.NotYetImplementedException

/**
 * 
 *
 * @author dkitchin
 */
object WriteJSON extends TotalSite1 with UntypedSite {

  def eval(a: AnyRef): AnyRef = {
    convertToJson(a)
  }
  
  def convertToJson(a: AnyRef): String = {
    throw new NotYetImplementedException("WriteJSON not yet implemented")
  }
  
}