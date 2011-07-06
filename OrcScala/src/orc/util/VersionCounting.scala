//
// VersionCounting.scala -- Scala class/trait/object VersionCounting
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
package orc.util

import orc.Versioned

/**
 * 
 * Mix in a threadsafe version counter.
 *
 * @author dkitchin
 */

// What about version number wraparound? The version stamp is currently a finite-precision integer.
  // This is probably only a concern for the root clock.
// TODO: Make version counting robust to overflow
// TODO: Add min-version and max-version reserved values
trait VersionCounting extends Versioned {
  val ctr = new java.util.concurrent.atomic.AtomicInteger(0)
  def version = ctr.get()
  def bump = ctr.incrementAndGet()
}