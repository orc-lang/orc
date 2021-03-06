//
// package.scala -- Scala package object for package orc.run.distrib.token
// Project OrcScala
//
// Created by jthywiss on Jan 19, 2016.
//
// Copyright (c) 2019 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib

package object token {
  type PeerLocation = RuntimeRef[OrcPeerCmd]
}
