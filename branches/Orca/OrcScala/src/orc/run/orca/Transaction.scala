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
package orc.run.orca

import orc.run.core.Subgroup
import orc.run.core.Blocker
import orc.run.core.Token

/**
 * 
 *
 * @author dkitchin
 */

class Transaction(init: Token) extends Subgroup(init.getGroup()) 
with TransactionalContext 
with Blocker 
with VersionCounting {
   
  val parentTransaction: TransactionalContext = init.getTxn()
  val initialVersion: Int = parentTransaction.version

  var commitValues: Set[AnyRef] = Set()
  var participants: Set[Participant] = Set()
  var status: TransactionStatus = Running

  def schedule() = init.runtime.schedule(this)
  
  def publish(t: Token, v: AnyRef) = {
    synchronized { commitValues += v }
    t.halt()
  }

  def onHalt() { 
    synchronized {
      status match {
        case Running => { status = BeginVote ; schedule() }
        case _ : Aborting => {}
      }
    }
  }
       
  override def kill() = abort()
  
  def join(p: Participant): Boolean = {
    synchronized {
      status match {
        case Running => { 
          participants += p
          true 
        }
        /* It is possible to receive a join request when in these states from a def class goal
         * that remains active after the class call returns and halts.
         */
        case _ : Preparing | _ : Committing => false
        /*
         * It is possible to be aborted early, while a call is still in flight,
         * or to receive a join request from a def class goal as stated above.
         */
        case _ : Aborting => false 
      }
    }
  }

  def abort() {
    synchronized {
      status match {
        case Running | _ : Preparing => {
          status = BeginAbort
          schedule()
        }
        case _ : Committing => {}
        case _ : Aborting => {}
      }
    }
  }

  

  override def run() {
    synchronized {
      status match {
        case BeginVote => {
          if (!participants.isEmpty) {
            status = VoteInProgress(participants.size)
            participants foreach { runtime.schedule(_) }
          }
          else {
            status = BeginCommit
            schedule()
          }
        }
        case BeginCommit => {
          parent.remove(this)
          val finalVersion = parentTransaction.bump
          if (!participants.isEmpty) {
            status = CommitInProgress(finalVersion, participants.size)
            participants foreach { runtime.schedule(_) }
          }
          else {
            status = Committed
            init.schedule()
          }
        }
        case BeginAbort => {
          super.kill()
          if (!participants.isEmpty) {
            status = AbortInProgress(participants.size)
            participants foreach { runtime.schedule(_) }
          }
          else {
            status = Aborted
            init.schedule()
          }
        }
      }
    }
  }

  def check(p: Participant) {
    (synchronized { status } ) match {
      case VoteInProgress(_) => {
        val vote = p.vote()
        if (vote) {
          // Vote to commit.
          synchronized {
            status match {
              case VoteInProgress(n) if (n > 1) => { 
                status = VoteInProgress(n-1) 
              }
              case VoteInProgress(1) => { 
                status = BeginCommit
                schedule() 
              }
              case _ : Aborting => {}
            }
          }
        }
        else {
          // Vote to abort.
          synchronized {
            status match {
              case VoteInProgress(n) if (n > 0) => { 
                status = BeginAbort
                schedule() 
              }
              case _ : Aborting => {}
            }
          }
        }
      }
      case CommitInProgress(finalVersion, _) => {
        p.commit(finalVersion)
        synchronized {
          status match {
            case CommitInProgress(finalVersion, n) if (n > 1) => { 
              status = CommitInProgress(finalVersion, n-1)
            }
            case CommitInProgress(finalVersion, 1) => { 
              status = Committed
              init.schedule()
            }
          }
        }
      }
      case AbortInProgress(_) => {
        p.abort()
        synchronized {
          status match {
            case AbortInProgress(n) if (n > 1) => { 
              status = AbortInProgress(n-1)
            }
            case AbortInProgress(1) => { 
              status = Aborted
              init.schedule()
            }
          }
        }
      }
    }
  }

  def check(t: Token) {
    status match {
      case Committed => { t.publishAll(commitValues) }
      case Aborted => { t.unblock() }
    }
  }
   
 
}


trait TransactionStatus
  
case object Running extends TransactionStatus

trait Preparing extends TransactionStatus
case object BeginVote extends Preparing
case class VoteInProgress(uncounted: Int) extends Preparing

trait Committing extends TransactionStatus
case object BeginCommit extends Committing
case class CommitInProgress(finalVersion: Int, uncounted: Int) extends Committing
case object Committed extends Committing

trait Aborting extends TransactionStatus
case object BeginAbort extends Aborting
case class AbortInProgress(uncounted: Int) extends Aborting
case object Aborted extends Aborting



