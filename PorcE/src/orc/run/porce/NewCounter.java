
package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.CounterNested;
import orc.run.porce.runtime.CounterService;
import orc.run.porce.runtime.CounterTerminator;
import orc.run.porce.runtime.PorcERuntime;

public abstract class NewCounter extends Expression {
    @Child
    protected Expression parent;

    protected final PorcERuntime runtime;

    public NewCounter(final PorcERuntime runtime, final Expression parent) {
        this.runtime = runtime;
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

        public Simple(final PorcERuntime runtime, final Expression parent, final Expression haltContinuation) {
            super(runtime, parent);
            this.haltContinuation = haltContinuation;
        }

        @Override
        public Counter executeCounter(final VirtualFrame frame) {
            try {
                return new CounterNested(runtime, parent.executeCounter(frame), haltContinuation.executePorcEClosure(frame));
            } catch (final UnexpectedResultException e) {
                throw InternalPorcEError.typeError(this, e);
            }
        }

        public static NewCounter create(final PorcERuntime runtime, final Expression parent, final Expression haltContinuation) {
            return new Simple(runtime, parent, haltContinuation);
        }
    }

    public final static class Service extends NewCounter {
        @Child
        protected Expression parentContaining;
        @Child
        protected Expression terminator;

        public Service(final PorcERuntime runtime, final Expression parentCalling, final Expression parentContaining, final Expression terminator) {
            super(runtime, parentCalling);
            this.parentContaining = parentContaining;
            this.terminator = terminator;
        }

        @Override
        public Counter executeCounter(final VirtualFrame frame) {
            try {
                return new CounterService(runtime, parent.executeCounter(frame), parentContaining.executeCounter(frame), terminator.executeTerminator(frame));
            } catch (final UnexpectedResultException e) {
                throw InternalPorcEError.typeError(this, e);
            }
        }

        public static NewCounter create(final PorcERuntime runtime, final Expression parentCalling, final Expression parentContaining, final Expression haltContinuation) {
            return new Service(runtime, parentCalling, parentContaining, haltContinuation);
        }
    }

    public final static class Terminator extends NewCounter {
        @Child
        protected Expression terminator;

        public Terminator(final PorcERuntime runtime, final Expression parent, final Expression terminator) {
            super(runtime, parent);
            this.terminator = terminator;
        }

        @Override
        public Counter executeCounter(final VirtualFrame frame) {
            try {
                return new CounterTerminator(runtime, parent.executeCounter(frame), terminator.executeTerminator(frame));
            } catch (final UnexpectedResultException e) {
                throw InternalPorcEError.typeError(this, e);
            }
        }

        public static NewCounter create(final PorcERuntime runtime, final Expression parent, final Expression haltContinuation) {
            return new Terminator(runtime, parent, haltContinuation);
        }
    }
}
