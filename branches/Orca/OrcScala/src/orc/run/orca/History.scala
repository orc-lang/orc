//
// History.scala -- Scala class/trait/object History
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
package orc.run.orca

import scala.collection.immutable.Queue
import orc.Handle

/**
 * 
 *
 * @author dkitchin
 */
/* 
 * A sequence of version-stamped values of type T. 
 * 
 * 
 */

class History[T] {
  
  trait HistoryState
  // This is written as a nullary case class due to a compiler bug:
  // https://issues.scala-lang.org/browse/SI-4593
  // TODO: Once this bug is fixed, revert these to case objects
  case class Available() extends HistoryState
  
  case class Frozen(freezer: Participant, frozenVersion: Int, waitingWrites: Queue[(T, Int, Handle)]) extends HistoryState {
    def enqueue(value: T, version: Int, h: Handle): Frozen = {
      Frozen(freezer, frozenVersion, waitingWrites :+ (value, version, h))
    }
  }

  
  /* A list of versioned values, maintained in sorted order, from
   * newest version (largest #) to oldest version (smallest #)
   */
  private var history: List[(Int, T)] = Nil
  
  /* The current state of this history. */
  private var state: HistoryState = Available()
  
  /*
   * Attempt to freeze this history at the given version v.
   * 
   * If the current max version in the history is less than v,
   * and the history is not currently frozen,
   * the history is synchronously frozen, 
   * and all writes block until it is thawed.
   * 
   * If the current max version in the history is greater than v,
   * or the history is already frozen,
   * then the freeze fails and the history state is unchanged.
   */
  def freeze(freezer: Participant, frozenVersion: Int): Boolean = {
    synchronized {
      state match {
        case Available() => { 
          history match {
            case (n,_)::_ if (n > frozenVersion) => { false }
            case _ => { state = Frozen(freezer, frozenVersion, Queue.empty); true }
          }
        }
        case _ : Frozen => false
      }
    }
  }
  
  /* Thaw the history, if it is frozen, 
   * and the requesting entity was responsible for the original freeze.
   * Do nothing if it is not frozen.
   */
  def thaw(p: Participant) {
    val thawedCalls =
      synchronized {
        state match {
          case Available() => { Nil }
          case Frozen(freezer, _, waitingWrites) if (p == freezer) => {
            (for ((value, version, h) <- waitingWrites) yield {
              history = insert(value, version, history)
              h 
            }).toSeq
          }
          case Frozen(freezer, _, _) if (p != freezer) => Nil
        }
      }
    for (h <- thawedCalls) { h.publish() }
  }
  
  def commit(p: Participant, finalValue: T, finalVersion: Int) = {
    val thawedCalls =
      synchronized {
        state match {
          case Frozen(freezer, _, waitingWrites) if (p == freezer) => {
            history = insert(finalValue, finalVersion, history)
            state = Available()
            waitingWrites map { 
              case (value, version, h) => { 
                history = insert(value, version, history)
                h 
              } 
            }
          }
        }
      }
    for (h <- thawedCalls) { h.publish() }
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
  def add(value: T, version: Int, h: Handle): Unit = {
    val unblocked =
      synchronized {
        state match {
          case Available() => { history = insert(value, version, history); true }
          case f : Frozen => { state = f enqueue (value, version, h); false }
        }
      }
    if (unblocked) { h.publish() }
  }
  
  def onEmpty(activity: => Unit) {
    synchronized {
      if (history.isEmpty) { activity }
    }
  }
  
    
  
  /*
   * Return true if this history has no entries,
   * false otherwise.
   */
  def isEmpty: Boolean = history.isEmpty
  
  
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
   
  
}