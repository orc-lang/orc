//
// TRef.scala -- Scala class/trait/object TRef
// Project Orca
//
// $Id$
//
// Created by dkitchin on Jun 29, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.orca

import orc.values.sites.TotalSite
import orc.values.sites.TransactionOnlySite
import orc.values.OrcRecord
import orc.Handle
import orc.TransactionInterface
import orc.error.NotYetImplementedException
import orc.error.runtime.ArityMismatchException
import orc.Participant

/**
 * 
 *
 * @author dkitchin
 */
object TRef extends TotalSite {

  def evaluate(args: List[AnyRef]): AnyRef = {
    val instance = {
      args match {
        case Nil => new TRefInstance(None)
        case List(a) => new TRefInstance(Some(a))
        case _ => throw new ArityMismatchException(1, args.size)
      }
    }
    new OrcRecord(
      "read" -> instance.readSite,
      "write" -> instance.writeSite
    )
  }
  
}


class TRefInstance(initialContents: Option[AnyRef]) {
  
  val repository = new Repository[AnyRef](initialContents)
  
  val readSite = new TransactionOnlySite {
    def call(args: List[AnyRef], h: Handle, tx: TransactionInterface) {
      repository.read(tx) match {
        case Some(v) => {
          h.publish(v)
        }
        case None => {
          // TODO: Implement blocking reads
          throw new NotYetImplementedException("Blocking reads on txn refs not yet implemented") 
        }
      }
    }
  }
  
  val writeSite = new TransactionOnlySite {
    def call(args: List[AnyRef], h: Handle, tx: TransactionInterface) {
      args match {
        case List(arg) => {
          repository.write(tx, args.head) // TODO: Add arity checking
          h.publish()
        }
        case _ => throw new ArityMismatchException(1, args.size) 
      }
    }
  }
  
}

/* 
 * A sequence of version-stamped values of type T. 
 * 
 * Adding a new version is a synchronized operation.
 * Reading a version is unsynchronized.
 * 
 */
class History[T] {
  
  /* A list of versioned values, maintained in sorted order, from
   * newest version (largest #) to oldest version (smallest #)
   */
  private var history: List[(Int, T)] = Nil 
  private var internalLock = new scala.concurrent.Lock()
  
  def lock() = { internalLock.acquire() }
  def unlock() = { internalLock.release() }
  
  /* Insert an entry into an ordered versioned list, preserving the order. 
   * Duplicate versions are not allowed; if a duplicate is found, an error is raised. 
   */
  private def insert(value: T, version: Int, tail: List[(Int, T)]): List[(Int, T)] = {
    tail match {
      case Nil => {
        List( (version, value) )
      }
      case (thisVersion, _) :: _ if (version >= thisVersion) => {
        assert(version != thisVersion, { "duplicate version # " + version + " in the same history." })
        (version, value) :: tail
      }
      case h::t => {
        h::(insert(value, version, t))
      }
    }
  }
  
  /* Read the value v at the given version; return Some(v).
   * It is possible that all versions in this history exceed the requested version;
   * in this case, return None. 
   */
  def lookup(version: Int): Option[T] = {
    history find { _._1 <= version } map { _._2 }
  }
  
  /*
   * Add a value to the history at the given version.
   * If the version already exists, an exception is raised.
   */
  def add(value: T, version: Int): Unit = {
    history = insert(value, version, history)
    //println("Content of history " + this + ": " + history)
  }
  
  /*
   * Return true if this history has no entries,
   * false otherwise.
   */
  def isEmpty: Boolean = history.isEmpty
  
  /* Read the most recent version in the history, or None if the history is empty. */
  // TODO: Replace None with a reserved min-version value.
  def clock: Option[Int] = {
    history match {
      case Nil => None
      case (n, _)::_ => Some(n)
    }
  }
  
  
}

class Repository[T](initialContents: Option[T]) {
    
  val txTable = new scala.collection.mutable.HashMap[TransactionInterface, History[T]]
  
  private def retrieve(tx: TransactionInterface, version: Int): Option[T] = {
    txTable get tx flatMap { _ lookup version } 
  }
  
  /* Find the history for this transaction.
   * If there is no history, create one.
   */
  // TODO: Check if the tx is live before synthesizing a history for it.
  private def forceLookup(tx: TransactionInterface): History[T] = { 
    txTable.synchronized {
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
    
  
  def read(hostTx: TransactionInterface): Option[T] = {
    def seek(thisTx: TransactionInterface, version: Int): Option[T] = {
      retrieve(thisTx, version) orElse { 
        thisTx.parentTransaction flatMap { 
          seek(_, thisTx.initialVersion) 
        }
      }
    }
    seek(hostTx, hostTx.version) orElse initialContents
  }
  
  def write(hostTx: TransactionInterface, newContent: T): Unit = {
    val targetHistory = forceLookup(hostTx)
    /* Wait for the history lock to prevent a write from occurring
     * during a child transaction commit.
     */
    targetHistory.lock()
    if (targetHistory.isEmpty) { hostTx join (new RepositoryParticipant(hostTx)) }
    targetHistory.add(newContent, hostTx.bump)
    targetHistory.unlock()
  }
  

  class RepositoryParticipant(hostTx: TransactionInterface) extends Participant {
    
    def prepare(): Option[Int => Unit] = {
      /* Determine the final value to be committed. */
      val finalValue = retrieve(hostTx, hostTx.version) getOrElse { throw new AssertionError("History for an active participant should not be empty.") }
      
      /* Determine the parent transaction to which to commit. */
      val parent = hostTx.parentTransaction getOrElse { throw new AssertionError("The root transaction has no parent.") }
      
      /* Find the parent history.
       * If there is no parent history, create one. 
       */
      val parentHistory = forceLookup(parent)
        
      /* Lock the parent history (to block parent writes), then check if it has advanced 
       * past the initial version of the committing transaction. 
       */
      parentHistory.lock()
      //println("Comparing host initial version " + hostTx.initialVersion + " to parent current version " + parentHistory.clock.getOrElse(-1))
      if (parentHistory.clock map { _ <= hostTx.initialVersion } getOrElse true) {
        /* Agree to commit, returning a commit thunk. */
        def onCommit(n: Int) = {
          parentHistory.add(finalValue, n)
          //println("committed txn " + hostTx)
          parentHistory.unlock()
        }
        Some(onCommit)
      }
      else {
        //println("aborted txn " + hostTx)
        /* Request abort. */
        None
      }
    }
    
    def rollback(): Unit = {
      /* Determine the parent transaction under which to roll back. */
      val parent = hostTx.parentTransaction getOrElse { throw new AssertionError("The root transaction has no parent.") }
      
      /* Unlock the parent history, if one exists. */
      txTable get parent map { _.unlock() }
      
      // TODO: Perform proper garbage collection of stale txtable entries.
    }
    
  }
  
}


