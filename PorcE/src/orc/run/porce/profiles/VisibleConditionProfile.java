//
// VisibleConditionProfile.java -- Truffle profile VisibleConditionProfile
// Project PorcE
//
// Created by amp on Oct 8, 2018.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce.profiles;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

/**
 *
 *
 * @author amp
 */
public class VisibleConditionProfile {
    @CompilationFinal
    private boolean wasTrue = false;
    @CompilationFinal
    private boolean wasFalse = false;

    /**
     * @param b
     * @return
     */
    public boolean profile(boolean b) {
        if (b) {
            if (!wasTrue) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                wasTrue = true;
            }
            return true;
        } else {
            if (!wasFalse) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                wasFalse = true;
            }
            return false;
        }
    }

    /**
     * @return
     */
    public boolean wasTrue() {
        return wasTrue;
    }

    /**
     * @return
     */
    public boolean wasFalse() {
        return wasFalse;
    }

    /**
     * @return
     */
    public static VisibleConditionProfile createBinaryProfile() {
        return new VisibleConditionProfile();
    }

    @SuppressWarnings("boxing")
    @Override
    public String toString() {
        return String.format("%s(wasTrue=%s, wasFalse=%s)@%08x", getClass().getSimpleName(), wasTrue, wasFalse, hashCode());
    }
}
