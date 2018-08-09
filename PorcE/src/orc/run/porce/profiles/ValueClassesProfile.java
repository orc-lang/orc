//
// Snippet.java -- Scala class/trait/object Snippet
// Project PorcE
//
// Created by amp on Jun 25, 2018.
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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ValueProfile;

/**
 * A profiler which profiles all elements of an array by class.
 *
 * @author amp
 */
public final class ValueClassesProfile {
    @CompilerDirectives.CompilationFinal(dimensions = 1)
    private volatile ValueProfile[] argumentTypeProfiles = null;

    @SuppressWarnings("boxing")
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_UNROLL)
    public Object[] profile(Object[] arguments) {
        CompilerAsserts.compilationConstant(arguments.length);
        CompilerAsserts.compilationConstant(this);

        if (argumentTypeProfiles == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            synchronized (this) {
                if (argumentTypeProfiles == null) {
                    ValueProfile[] profiles = new ValueProfile[arguments.length];
                    for (int i = 0; i < profiles.length; i++) {
                        profiles[i] = ValueProfile.createClassProfile();
                    }
                    argumentTypeProfiles = profiles;
                }
            }
        }

        final ValueProfile[] profiles = argumentTypeProfiles;

        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = profiles[i].profile(arguments[i]);
        }

        return arguments;
    }

}

