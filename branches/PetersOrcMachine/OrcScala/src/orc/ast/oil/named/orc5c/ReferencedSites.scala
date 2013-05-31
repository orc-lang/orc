//
// ReferencedSites.scala -- Scala class/trait/object ReferencedSites
// Project OrcScala
//
// $Id$
//
// Created by amp on May 28, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named.orc5c

import orc.values.sites.Site

/**
  *
  * @author amp
  */
trait ReferencedSites {
  self: Orc5CAST =>

  lazy val referencedSites: Set[Site] = {
    val set = new scala.collection.mutable.HashSet[Site]()
    val collect = new Orc5CASTTransform {
      override def onArgument(context: List[BoundVar]) = {
        case x@Constant(s : Site) => set += s; x
      }
    }
    collect(this)
    Set.empty ++ set
  }
}