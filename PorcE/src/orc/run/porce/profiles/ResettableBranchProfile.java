//
// ResettableBranchProfile.java -- Truffle profile ResettableBranchProfile
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
 * A branch profile (similar to BranchProfile) which supports resetting.
 *
 * Methods on this profile race against one another for performance reasons. None of the races should prevent the
 * profile from stablizing.
 *
 * @author amp
 */
public final class ResettableBranchProfile {
    private ResettableBranchProfile() {
    }

    @CompilationFinal
    private boolean visited = false;

    /**
     * Enter the branch being profiled.
     *
     * This call may return true any number of times, but will only return true after a transfer to interpreter (and
     * invalidate).
     *
     * @return true iff this is the profile invalidated for this call.
     */
    public boolean enter() {
        CompilerAsserts.compilationConstant(this);
        if (!visited) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            visited = true;
            return true;
        }
        return false;
    }

    /**
     * Reset this profile to be unvisited.
     *
     * This is useful when one profile triggering invalidates the state of another profile.
     *
     * This should only be called a fixed number of times per instance to guarantee stabilization. This implies that
     * there should be no cycles between profiles which reset each other.
     *
     * This must only be called from the interpreter. For instance, after #enter() returned true.
     */
    public void reset() {
        CompilerAsserts.neverPartOfCompilation(
                "Only call ResettableBranchProfile.reset() in a transferToInterpreterAndInvalidate branch.");
        visited = false;
    }

    @Override
    public String toString() {
        return "ResettableBranchProfile(visited=" + visited + ")@" + Integer.toHexString(this.hashCode());
    }

    static public ResettableBranchProfile create() {
        return new ResettableBranchProfile();
    }
}
