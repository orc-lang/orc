//
// TransactionalContext.scala -- Scala class/trait/object TransactionalContext
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
 * 
 * The interface through which the environment interacts with transactions.
 *
 * @author dkitchin
 */

trait TransactionalContext extends Versioned {
  def join(p: Participant): Boolean
  def abort(): Unit
}