//
// And.java -- Java class And
// Project OrcScala
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.bool;

/**
 * @author dkitchin
 */
public class And extends BoolBinopSite {

    @Override
    public boolean compute(final boolean a, final boolean b) {
        return a && b;
    }

}
