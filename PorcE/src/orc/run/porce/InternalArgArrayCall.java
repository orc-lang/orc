
package orc.run.porce;

import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;

abstract class InternalArgArrayCallBase extends Expression {
    protected final PorcEExecutionRef execution;

    public InternalArgArrayCallBase(final PorcEExecutionRef execution) {
        this.execution = execution;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        CompilerDirectives.bailout("InternalArgArrayCall cannot be executed this way.");
        throw new IllegalStateException("InternalArgArrayCall cannot be executed this way.");
    }

    public abstract Object execute(VirtualFrame frame, Object target, Object[] arguments);

    protected static InternalArgArrayCallBase findCacheRoot(final InternalArgArrayCallBase n) {
        CompilerAsserts.neverPartOfCompilation();
        if (n.getParent() instanceof InternalArgArrayCallBase) {
            return findCacheRoot((InternalArgArrayCallBase) n.getParent());
        } else {
            return n;
        }
    }

    public static InternalArgArrayCallBase create(final PorcEExecutionRef execution) {
        return InternalArgArrayCall.create(execution);
    }
}

public class InternalArgArrayCall extends InternalArgArrayCallBase {
    public InternalArgArrayCall(final PorcEExecutionRef execution) {
        super(execution);
    }

    private int cacheSize = 0;
    private static int cacheMaxSize = 4;

    @Override
    public Object execute(final VirtualFrame frame, final Object target, final Object[] arguments) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final PorcEClosure t = (PorcEClosure) target;
        InternalArgArrayCallBase n;

        final Lock lock = getLock();
        lock.lock();
        try {
            if (cacheSize < cacheMaxSize) {
                cacheSize++;
                n = new Specific(t, (InternalArgArrayCallBase) this.copy(), execution);
                replace(n, "InternalArgArrayCall: Speculate on target closure.");
            } else {
                n = new Universal(new InternalArgArrayCallBase(execution) {
                    @Override
                    public Object execute(final VirtualFrame frame, final Object target, final Object[] arguments) {
                        CompilerDirectives.transferToInterpreter();
                        throw new AssertionError("This node should never be reached.");
                    }
                }, execution);
                findCacheRoot(this).replace(n, "Closure cache too large. Falling back to universal invocation.");
            }
        } finally {
            lock.unlock();
        }
        return n.execute(frame, target, arguments);
    }

    @Override
    public NodeCost getCost() {
        return NodeCost.UNINITIALIZED;
    }

    public static InternalArgArrayCall create(final PorcEExecutionRef execution) {
        return new InternalArgArrayCall(execution);
    }

    protected static class Specific extends InternalArgArrayCallBase {
        @Child
        protected InternalArgArrayCallBase notMatched;
        @Child
        protected DirectCallNode callNode;
        private final PorcEClosure expectedTarget;

        public Specific(final PorcEClosure expectedTarget, final InternalArgArrayCallBase notMatched, final PorcEExecutionRef execution) {
            super(execution);
            this.notMatched = notMatched;
            this.callNode = Truffle.getRuntime().createDirectCallNode(expectedTarget.body);
            this.expectedTarget = expectedTarget;
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object target, final Object[] arguments) {
            if (target instanceof PorcEClosure && expectedTarget.body == ((PorcEClosure) target).body) {
                arguments[0] = ((PorcEClosure) target).environment;
                return callNode.call(arguments);
            } else {
                return notMatched.execute(frame, target, arguments);
            }
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.POLYMORPHIC;
        }
    }

    protected static class Universal extends InternalArgArrayCallBase {
        @Child
        protected InternalArgArrayCallBase notMatched;
        @Child
        protected IndirectCallNode callNode;

        public Universal(final InternalArgArrayCallBase notMatched, final PorcEExecutionRef execution) {
            super(execution);
            this.notMatched = notMatched;
            this.callNode = Truffle.getRuntime().createIndirectCallNode();
        }

        @Override
        public Object execute(final VirtualFrame frame, final Object target, final Object[] arguments) {
            if (target instanceof PorcEClosure) {
                arguments[0] = ((PorcEClosure) target).environment;
                return callNode.call(((PorcEClosure) target).body, arguments);
            } else {
                return notMatched.execute(frame, target, arguments);
            }
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.POLYMORPHIC;
        }
    }

}
