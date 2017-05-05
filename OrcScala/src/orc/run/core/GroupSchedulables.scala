//
// GroupSchedulables.scala -- Schedulable tasks that act on Groups
// Project OrcScala
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.run.core

import orc.Schedulable

class GroupOnHalt(group: Group) extends Schedulable {
  def run(): Unit = orc.util.Profiler.measureInterval(0L, 'Group_onHalt) {
    group.onHalt()
  }

  override val nonblocking = false
}

class GroupOnDiscorporate(group: Group) extends Schedulable {
  def run(): Unit = orc.util.Profiler.measureInterval(0L, 'Group_onDiscorporate) {
    group.onDiscorporate()
  }

  override val nonblocking = false
}
