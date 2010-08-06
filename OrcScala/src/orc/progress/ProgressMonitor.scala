//
// ProgressMonitor.scala -- Scala class/trait/object ProgressMonitor
// Project OrcScala
//
// $Id$
//
// Created by jthywiss on Aug 5, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.progress

/**
 * Long-running task progress monitoring interface.
 * Inspired by Eclipse's core.runtime.SubMonitor.
 *
 * @author quark, jthywiss
 */
trait ProgressMonitor {
  type WorkQty = Int
  def setTaskName(name: String): Unit
  def setWorkRemaining(remainWorkQty: WorkQty): Unit
  
  def worked(completedWorkIncrement: WorkQty): Unit
  def newChild(delegatedWorkQty: WorkQty): ProgressMonitor
  
  def isCanceled(): Boolean
  def setBlocked(reason: String): Unit
  def clearBlocked(): Unit
}


/**
 * A ProgressMonitor that ignores all updates.
 *
 * @author jthywiss
 */
object NullProgressMonitor extends ProgressMonitor  {
  def setTaskName(name: String) { }
  def setWorkRemaining(remainWorkQty: WorkQty) { }
  
  def worked(completedWorkIncrement: WorkQty) { }
  def newChild(delegatedWorkQty: WorkQty) = this
  
  def isCanceled() = false
  def setBlocked(reason: String) { }
  def clearBlocked() { }
}
