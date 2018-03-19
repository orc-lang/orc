//
// NewCounter.java -- Truffle node NewCounter
// Project PorcE
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.porce;

import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.CounterNested;
import orc.run.porce.runtime.CounterService;
import orc.run.porce.runtime.CounterTerminator;
import orc.run.porce.runtime.PorcEExecution;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public abstract class NewCounter extends Expression {
    @Child
    protected Expression parent;

    protected final PorcEExecution execution;

    public NewCounter(final PorcEExecution execution, final Expression parent) {
        this.execution = execution;
        this.parent = parent;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return executeCounter(frame);
    }

    @Override
    public abstract Counter executeCounter(VirtualFrame frame);

    public final static class Simple extends NewCounter {
        @Child
        protected Expression haltContinuation;

        public Simple(final PorcEExecution execution, final Expression parent, final Expression haltContinuation) {
            super(execution, parent);
            this.haltContinuation = haltContinuation;
        }

        @Override
        public Counter executeCounter(final VirtualFrame frame) {
            try {
                return new CounterNested(execution, parent.executeCounter(frame), haltContinuation.executePorcEClosure(frame));
            } catch (final UnexpectedResultException e) {
                throw InternalPorcEError.typeError(this, e);
            }
        }

        public static NewCounter create(final PorcEExecution execution, final Expression parent, final Expression haltContinuation) {
            return new Simple(execution, parent, haltContinuation);
        }
    }

    public final static class Service extends NewCounter {
        @Child
        protected Expression parentContaining;
        @Child
        protected Expression terminator;

        public Service(final PorcEExecution execution, final Expression parentCalling, final Expression parentContaining, final Expression terminator) {
            super(execution, parentCalling);
            this.parentContaining = parentContaining;
            this.terminator = terminator;
        }

        @Override
        public Counter executeCounter(final VirtualFrame frame) {
            try {
                return new CounterService(execution, parent.executeCounter(frame), parentContaining.executeCounter(frame), terminator.executeTerminator(frame));
            } catch (final UnexpectedResultException e) {
                throw InternalPorcEError.typeError(this, e);
            }
        }

        public static NewCounter create(final PorcEExecution execution, final Expression parentCalling, final Expression parentContaining, final Expression haltContinuation) {
            return new Service(execution, parentCalling, parentContaining, haltContinuation);
        }
    }

    public final static class Terminator extends NewCounter {
        @Child
        protected Expression terminator;

        public Terminator(final PorcEExecution execution, final Expression parent, final Expression terminator) {
            super(execution, parent);
            this.terminator = terminator;
        }

        @Override
        public Counter executeCounter(final VirtualFrame frame) {
            try {
                return new CounterTerminator(execution, parent.executeCounter(frame), terminator.executeTerminator(frame));
            } catch (final UnexpectedResultException e) {
                throw InternalPorcEError.typeError(this, e);
            }
        }

        public static NewCounter create(final PorcEExecution execution, final Expression parent, final Expression haltContinuation) {
            return new Terminator(execution, parent, haltContinuation);
        }
    }
}
