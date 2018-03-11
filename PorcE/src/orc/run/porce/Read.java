//
// Read.java -- Truffle nodes Read.*
// Project PorcE
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

public class Read {
    private Read() {
    }

    public static class Constant extends Expression {
        private final Object value;

        public Constant(final Object value) {
            this.value = value;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return value;
        }

        public static Constant create(final Object v) {
            return new Constant(v);
        }
    }

    public static class Local extends Expression {
        private final FrameSlot slot;

        public Local(final FrameSlot slot) {
            this.slot = slot;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            Object value;
            try {
                value = frame.getObject(slot);
            } catch (final FrameSlotTypeException e) {
                throw InternalPorcEError.typeError(this, e);
            }
            return value;
        }

        public static Local create(final FrameSlot slot) {
            assert slot != null;
            return new Local(slot);
        }
    }

    public static class Argument extends Expression {
        private final int index;

        public Argument(final int index) {
            this.index = index;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            final Object value = frame.getArguments()[index];
            return value;
        }

        public static Argument create(final int index) {
            assert index >= 0;
            return new Argument(index + 1);
        }
    }

    public static class Closure extends Expression {
        private final int index;

        public Closure(final int index) {
            this.index = index;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            // TODO: PERFORMANCE: Storing the closure in a frame slot would save an indexed load per node.
            final Object value = ((Object[]) frame.getArguments()[0])[index];
            return value;
        }

        public static Closure create(final int index) {
            assert index >= 0;
            return new Closure(index);
        }
    }
}
