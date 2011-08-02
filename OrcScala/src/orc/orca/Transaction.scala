//
// Transaction.scala -- Scala class/trait/object Transaction
// Project Orca
//
// $Id$
//
// Created by dkitchin on Jul 21, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.orca

/**
 * 
 *
 * @author dkitchin
 */
trait Transaction extends Versioned {
  val initialVersion: Int
  val parentTransaction: Option[Transaction]
  
  def join(p: Participant): Boolean
  def enqueue(p: Participant): Unit
  def abort(): Unit
  
  def isRoot: Boolean = parentTransaction.isEmpty
}


/* The root transaction is not actually a transactional context.
 * It is simply responsible for maintaining global version counting
 * and providing the nontransactional view of all transactional resources.
 */
trait RootTransaction extends Transaction {
  val initialVersion = 0
  val parentTransaction = None
  def join(p: Participant) = false /* Participants cannot join the root */
  def abort() { } /* The root can't be aborted */
}