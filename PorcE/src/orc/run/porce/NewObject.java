//
// NewObject.java -- Truffle node NewObject
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.runtime.PorcEObject;
import orc.values.Field;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class NewObject extends Expression {
    @Children
    protected final Expression[] expressions;
    private final Field[] fields;

    public NewObject(final Field[] fields, final Expression[] expressions) {
        this.fields = fields;
        this.expressions = expressions;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return executePorcEObject(frame);
    }

    @Override
    @ExplodeLoop
    public PorcEObject executePorcEObject(final VirtualFrame frame) {
        final Object[] values = new Object[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
            values[i] = expressions[i].execute(frame);
        }
        return new PorcEObject(fields, values);
    }

    public static NewObject create(final Field[] fields, final Expression[] expressions) {
        return new NewObject(fields, expressions);
    }
}
