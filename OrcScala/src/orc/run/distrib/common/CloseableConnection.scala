//
// CloseableConnection.scala -- Scala trait CloseableConnection
// Project OrcScala
//
// Created by jthywiss on Feb 10, 2019.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib.common

trait ClosableConnection {
  def close(): Unit
  def abort(): Unit
}
