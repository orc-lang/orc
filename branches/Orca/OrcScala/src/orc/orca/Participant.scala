//
// Participant.scala -- Scala class/trait/object Participant
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

import orc.Schedulable

/**
 * 
 *
 * @author dkitchin
 */
trait Participant extends Schedulable {
  
  val context: Transaction

  def moveToVote(vote: Boolean => Unit): Participant
  def moveToCommit(finalVersion: Int, done: () => Unit): Participant
  def moveToAbort(done: () => Unit): Participant
  
}