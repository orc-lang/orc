//
// PorcEUnit.java -- Java singleton PorcEUnit
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

public class PorcEUnit {
    private PorcEUnit() {
    }

    public static final PorcEUnit SINGLETON = new PorcEUnit();
}
