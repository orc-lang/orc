//
// MaximumValueProfile.java -- Truffle profile MaximumValueProfile
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.profiles;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

/**
 * A value profile which tracks the maximum value observed.
 *
 * @author amp
 */
public final class SingleBranchProfile {
    private SingleBranchProfile() {
    }

    @CompilationFinal
    private long mask = 0;
    @CompilationFinal
    private boolean singleBranch = true;

    /**
     * @return true if this profile has only observed one branch, false otherwise.
     */
    public boolean isSingleBranch() {
        return singleBranch;
    }

    /**
     * Profile the value and return the maximum value observed.
     *
     * This will invalidate if v is greater than the compiled in maximum.
     */
    public void enter(int path) {
        CompilerAsserts.compilationConstant(this);
        long pathMask = 1L << path;
        if ((pathMask & mask) > 0) {
            return;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (pathMask <= 0 || Long.bitCount(pathMask) != 1) {
                throw new IllegalArgumentException("SingleBranchProfile.enter must be passed a number between 0 and 63.");
            }
            if (mask != 0) {
                singleBranch = false;
            }
            mask &= pathMask;
        }
    }

    @Override
    public String toString() {
        return "SingleBranchProfile(mask=" + mask + ")@" + Integer.toHexString(this.hashCode());
    }

    static public SingleBranchProfile create() {
        return new SingleBranchProfile();
    }
}
