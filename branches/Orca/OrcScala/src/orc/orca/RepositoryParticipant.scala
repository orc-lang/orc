//
// RepositoryParticipant.scala -- Scala class/trait/object RepositoryParticipant
// Project Orca
//
// $Id$
//
// Created by dkitchin on Jul 22, 2011.
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

trait ParticipantStatus

// This is written as a nullary case class due to a compiler bug:
// https://issues.scala-lang.org/browse/SI-4593
// TODO: Once this bug is fixed, revert to a case object
case class Running() extends ParticipantStatus

case class Preparing(castVote: Boolean => Unit) extends ParticipantStatus
case class Prepared() extends ParticipantStatus
case class Committing(finalVersion: Int, done: () => Unit) extends ParticipantStatus
case class Aborting(done: () => Unit) extends ParticipantStatus


class RepositoryParticipant[T](val context: Transaction, val repos: Repository[T]) extends Participant {
    
    val targetTransaction = context.parentTransaction.get
    val targetHistory = repos.forceLookup(targetTransaction)
    val contextHistory = repos.forceLookup(context)
    var state: ParticipantStatus = Running()
    
    
    def run() {
      synchronized { state match {
        case Running() => { }
        case Preparing(castVote) => {
          /* Freeze the target history at this committing transaction's initial version. 
           * If this succeeds, vote to commit.
           * If the target history has advanced past that version already, vote to abort. 
           */
          val vote = targetHistory.freeze(this, context.initialVersion)
          state = Prepared()
          castVote(vote)
        }
        case Committing(finalVersion, done) => {
          // This repository becomes a participant for the parent.
          if (!targetTransaction.isRoot) {
            targetHistory onEmpty { 
              targetTransaction join (new RepositoryParticipant(targetTransaction, repos)) 
            }
          }
          
          /* Determine the final value to be committed. */
          val finalValue = contextHistory lookup context.version getOrElse {
            throw new AssertionError("History for an active participant should have at least one visible write.")
          }
          
          /* Commit. This will also thaw the target history. */
          targetHistory.commit(this, finalValue, finalVersion)
          
          done()
        }
        case Aborting(done) => {
          /* Thaw the target history. */
          targetHistory.thaw(this)
          
          done()
        }
      } }
    }
    
    
    def moveToVote(castVote: Boolean => Unit): Participant = {
      synchronized {
        state match {
          case Running() => { state = Preparing(castVote) }
          case Aborting(_) => { }
        }
      }
      this
    }
    
    def moveToCommit(finalVersion: Int, done: () => Unit): Participant = {
      synchronized {
        state match {
          case Prepared() => { state = Committing(finalVersion, done) }
        }
      }
      this
    }
    
    def moveToAbort(done: () => Unit): Participant = {
      synchronized {
        state match {
          case Running() | Preparing(_) | Prepared() => { state = Aborting(done) }
        }
      }
      this
    }
    
    
  }