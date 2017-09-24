//
// DOrcLocationPolicy.java -- Java interface DOrcLocationPolicy
// Project OrcScala
//
// Created by jthywiss on Sep 24, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.distrib;

import scala.collection.immutable.Set;

/**
 * Provides the set of Locations where this value may feasibly reside.
 *
 * @author jthywiss
 */
public interface DOrcLocationPolicy {
    public <L extends AbstractLocation> Set<L> permittedLocations(ClusterLocations<L> locations);
}
