//
// NewFuture.java -- Truffle node NewFuture
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Instrumentable;

@Instrumentable(factory = NewFutureWrapper.class)
public class NewFuture extends Expression {
    private final boolean raceFreeResolution;

    public NewFuture(final boolean raceFreeResolution) {
        super();
        this.raceFreeResolution = raceFreeResolution;
    }

    protected NewFuture(final NewFuture orig) {
        this(orig.raceFreeResolution);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return executeFuture(frame);
    }

    @Override
    public orc.Future executeFuture(final VirtualFrame frame) {
        return new orc.run.porce.runtime.Future(raceFreeResolution);
    }

    public static NewFuture create(final boolean raceFreeResolution) {
        return new NewFuture(raceFreeResolution);
    }
}
