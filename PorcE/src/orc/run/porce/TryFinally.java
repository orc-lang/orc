//
// TryFinally.java -- Truffle node TryFinally
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

public class TryFinally extends Expression {
    @Child
    protected Expression body;
    @Child
    protected Expression handler;

    public TryFinally(final Expression body, final Expression handler) {
        this.body = body;
        this.handler = handler;
    }

    @Override
    public void setTail(boolean b) {
        super.setTail(b);
        handler.setTail(b);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            return body.execute(frame);
        } finally {
            handler.execute(frame);
        }
    }

    public static TryFinally create(final Expression body, final Expression handler) {
        return new TryFinally(body, handler);
    }
}
