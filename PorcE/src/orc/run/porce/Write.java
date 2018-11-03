//
// Write.java -- Truffle nodes Write.*
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
import com.oracle.truffle.api.frame.VirtualFrame;

public class Write {
    private Write() {
    }

    public static class Local extends Expression {
        private final FrameSlot slot;
        @Child
        protected Expression value;

        protected Local(final FrameSlot slot, final Expression value) {
            this.slot = slot;
            this.value = value;
        }

        @Override
        public void setTail(boolean v) {
            super.setTail(v);
            if (value != null) {
                value.setTail(v);
            }
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            if (value == null) {
                frame.setObject(slot, null);
            } else {
                frame.setObject(slot, value.execute(frame));
            }
            return null;
        }

        public static Local create(final FrameSlot slot, final Expression value) {
            return new Local(slot, value);
        }
    }
}
