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
package orc.run.orca

/**
 * 
 *
 * @author dkitchin
 */

class RepositoryParticipant[T](val context: Transaction, val repos: Repository[T]) extends Participant {
    
    val targetTransaction = context.parentTransaction
    val targetHistory = repos.forceLookup(targetTransaction)
    val contextHistory = repos.forceLookup(context)

    def vote(): Boolean = {
      targetHistory.freeze(this, context.initialVersion)
    }
    
    def commit(finalVersion: Int) {
      // This repository becomes a participant for the parent.
      targetTransaction match {
        case tx: Transaction => {
          targetHistory onEmpty { 
            tx join (new RepositoryParticipant(tx, repos)) 
          }
        }
        case rc: RootContext => {}
      }
      
      /* Determine the final value to be committed. */
      val finalValue = contextHistory lookup context.version getOrElse {
        throw new AssertionError("History for an active participant should have at least one visible write.")
      }
      
      /* Commit. This will also thaw the target history. */
      targetHistory.commit(this, finalValue, finalVersion)
    }
    
    def abort() {
      /* Thaw the target history. */
      targetHistory.thaw(this)
    }
    
  }