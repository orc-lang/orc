//
// Subgroup.scala -- Scala class/trait/object Subgroup
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Aug 12, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core
import orc.OrcEvent

/**
 * 
 *
 * @author dkitchin
 */
abstract class Subgroup(parent: Group) extends Group {

  override def kill() = synchronized { super.kill(); parent.remove(this) }
  
  def notifyOrc(event: OrcEvent) = parent.notifyOrc(event)

  override val root = parent.root
  
  val runtime = parent.runtime

  parent.add(this)

}