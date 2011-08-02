//
// Repository.scala -- Scala class/trait/object Repository
// Project Orca
//
// $Id$
//
// Created by dkitchin on Jul 19, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.orca

import orc.Handle

/**
 * 
 *
 * @author dkitchin
 */
class Repository[T](initialContents: T) {
    
  val txTable = new scala.collection.mutable.HashMap[Transaction, History[T]]
  
  /* Retrieve the value at the given version in this transaction's history */
  private def retrieve(tx: Transaction, version: Int): Option[T] = {
    txTable synchronized { txTable get tx } flatMap { _ lookup version } 
  }
  
  
  /*
  /* Remove the history for transaction tx from the table entirely.
   * Look up the value at the given version number and return it.
   */
  def extract(tx: Transaction, version: Int): Option[T] = {
    txTable synchronized { txTable remove tx } flatMap { _ lookup version } 
  }
  */
  
  /* Find the history for this transaction in the repository.
   * If there is no history, create one.
   */
  def forceLookup(tx: Transaction): History[T] = { 
    txTable synchronized {
      txTable get tx match {
        case Some(history) => history
        case None => {
          val newHistory = new History[T]()
          txTable put (tx, newHistory)
          newHistory
        }
      }
    }
  }
    
  /*
   * Read the repository value for this transaction,
   * based on the transaction's current version,
   * walking up the repository tree if the value cannot be
   * found in this transaction's own history.
   */
  def read(hostTx: Transaction): T = {
    def seek(thisTx: Transaction, version: Int): Option[T] = {
      retrieve(thisTx, version) orElse { 
        thisTx.parentTransaction flatMap { 
          seek(_, thisTx.initialVersion) 
        }
      }
    }
    seek(hostTx, hostTx.version) getOrElse initialContents
  }
  
  /*
   * Read the repository value for this transaction,
   * based on the transaction's current version,
   * walking up the repository tree if the value cannot be
   * found in this transaction's own history.
   */
  def write(hostTx: Transaction, newContent: T, h: Handle): Unit = {
    val targetHistory = forceLookup(hostTx)
    targetHistory onEmpty { hostTx join (new RepositoryParticipant(hostTx, this)) }
    targetHistory.add(newContent, hostTx.bump, h)
  }
  

  
  
}