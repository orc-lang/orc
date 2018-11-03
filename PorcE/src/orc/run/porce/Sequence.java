//
// Sequence.java -- Truffle node Sequence
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public class Sequence extends Expression {
    @Children
    protected final Expression[] exprs;

    @CompilationFinal(dimensions = 1)
    protected FrameSlot[] scopedSlots;

    public Sequence(final Expression[] exprs, final FrameSlot[] scopedSlots) {
        this.exprs = exprs;
        this.scopedSlots = scopedSlots;
    }

    @Override
    public void setTail(boolean b) {
        super.setTail(b);
        for(int i = 0; i < exprs.length - 1; i++) {
            assert !exprs[i].isTail;
        }
        exprs[exprs.length - 1].setTail(b);
    }

    @Override
    @ExplodeLoop
    public Object execute(final VirtualFrame frame) {
        try {
            for (int i = 0; i < exprs.length - 1; i++) {
                final Expression expr = exprs[i];
                expr.executePorcEUnit(frame);
            }
            return exprs[exprs.length - 1].execute(frame);
        } finally {
            for (int i = 0; i < scopedSlots.length; i++) {
                frame.setObject(scopedSlots[i], null);
            }
        }
    }

    /**
     * Smart constructor for Sequence objects.
     */
    public static Expression create(final Expression[] exprs, final FrameSlot[] scopedSlots) {
        // Collect all elements of exprs and sub sequences there from
        final List<Expression> exs = new ArrayList<>(exprs.length);

        final List<FrameSlot> slots = new ArrayList<>(exprs.length);
        slots.addAll(Arrays.asList(scopedSlots));

        for (int i = 0; i < exprs.length - 1; i++) {
            Expression expr = exprs[i];
            if (expr instanceof Sequence && ((Sequence)expr).scopedSlots.length == 0) {
                // Sequences WITHOUT scopedSlots
                exs.addAll(Arrays.asList(((Sequence) expr).exprs));
            } else {
                exs.add(expr);
            }
        }
        // Last element
        {
            Expression expr = exprs[exprs.length - 1];
            if (expr instanceof Sequence) {
                Sequence seq = (Sequence) expr;
                slots.addAll(Arrays.asList(seq.scopedSlots));
                exs.addAll(Arrays.asList(seq.exprs));
            } else {
                exs.add(expr);
            }
        }

        assert exs.size() > 0;

        if (exs.size() == 1 && scopedSlots.length == 0) {
            return exs.get(0);
        } else {
            return new Sequence(exs.toArray(new Expression[exs.size()]), slots.toArray(new FrameSlot[slots.size()]));
        }
    }
}
