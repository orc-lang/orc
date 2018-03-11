//
// NewTerminator.java -- Truffle node NewTerminator
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.TerminatorNested;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class NewTerminator extends Expression {
    @Child
    protected Expression parent;

    public NewTerminator(final Expression parent) {
        this.parent = parent;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return executeTerminator(frame);
    }

    @Override
    public Terminator executeTerminator(final VirtualFrame frame) {
        try {
            return new TerminatorNested(parent.executeTerminator(frame));
        } catch (final UnexpectedResultException e) {
            throw new Error(e);
        }
    }

    public static NewTerminator create(final Expression parent) {
        return new NewTerminator(parent);
    }
}
