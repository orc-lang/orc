//
// IfLenientMethod.java -- Truffle node IfLenientMethod
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.runtime.PorcEClosure;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

public class IfLenientMethod extends Expression {
    @Child
    protected Expression argument;
    @Child
    protected Expression left;
    @Child
    protected Expression right;
    
    protected final ConditionProfile profile = ConditionProfile.createBinaryProfile(); 

    public IfLenientMethod(final Expression argument, final Expression left, final Expression right) {
        this.argument = argument;
        this.left = left;
        this.right = right;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        final Object d = argument.execute(frame);
        // TODO: Add support for external defs if we every have them supported in the API.
        if (profile.profile(d instanceof PorcEClosure)) {
            return left.execute(frame);
        } else {
            return right.execute(frame);
        }
    }

    public static IfLenientMethod create(final Expression argument, final Expression left, final Expression right) {
        return new IfLenientMethod(argument, left, right);
    }
}
