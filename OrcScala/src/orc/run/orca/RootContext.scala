//
// RootContext.scala -- Scala class/trait/object RootContext
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Sep 7, 2011.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.orca

/**
 * The root context is not actually a transaction.
 * It is simply responsible for maintaining global version counting
 * and providing the nontransactional view of all transactional resources. 
 *
 * @author dkitchin
 */
class RootContext extends TransactionalContext with VersionCounting {
  def join(p: Participant) = false /* Participants cannot join the root */
  def abort() { } /* The root can't be aborted */
}
