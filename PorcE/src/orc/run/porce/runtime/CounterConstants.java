//
// CounterConstants.java -- Java static class CounterConstants
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.runtime;

/** 
 * Class to help Graal constant fold configuration values used by Counter.
 * 
 * @author amp
 */
public abstract class CounterConstants {
    public static final boolean enableTiming = false;

    // Due to inlining, changing this will likely require a full rebuild.
    public static final boolean tracingEnabled = false;
    
    // FIXME: Make this configurable.
    public static final int maxCounterDepth = 8000;
}
