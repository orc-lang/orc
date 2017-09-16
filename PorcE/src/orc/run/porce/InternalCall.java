
package orc.run.porce;

import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.RootNode;

import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;

class InternalCallBase extends CallBase {
    public InternalCallBase(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
        super(target, arguments, execution);
    }

    protected Object[] buildArgumentValues(final VirtualFrame frame, final PorcEClosure t) {
        final Object[] argumentValues = new Object[arguments.length + 1];
        argumentValues[0] = t.environment;
        executeArguments(argumentValues, 1, 0, frame);
        return argumentValues;
    }
}

public class InternalCall extends CallBase {
    private int cacheSize = 0;
    private static int cacheMaxSize = 4;

    public InternalCall(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
        super(target, arguments, execution);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final PorcEClosure t = executeTargetClosure(frame);
        CallBase n;

        final Lock lock = getLock();
        lock.lock();
        try {
            if (cacheSize < cacheMaxSize) {
                cacheSize++;
                CallBase thisCopy = (CallBase) this.copy();
                thisCopy.setTail(isTail);
                if (isTail && getRootNode() == t.body.getRootNode()) {
                	// Self tail call
                	n = new SelfTail((Expression) target.copy(), t.body.getRootNode(), copyExpressionArray(arguments), thisCopy, execution);
                } else {
                	// Specialize for this target
                	n = new Specific((Expression) target.copy(), t, copyExpressionArray(arguments), thisCopy, execution);
                }
                Logger.info(() -> "At " + porcNode() + ": Replaced " + this + " with " + n + " (" + isTail + ", " + getRootNode() + " == " + t.body.getRootNode() + ")");
                replace(n, "InternalCall: Speculate on target closure.");
            } else {
                n = new Universal(target, arguments, execution);
                findCacheRoot(this).replace(n, "Closure cache too large. Falling back to universal invocation.");
            }
        } finally {
            lock.unlock();
        }
        return n.execute(frame);
    }

    @Override
    public NodeCost getCost() {
        return NodeCost.UNINITIALIZED;
    }

    private static CallBase findCacheRoot(final CallBase n) {
        if (n.getParent() instanceof Specific) {
            return findCacheRoot((Specific) n.getParent());
        } else {
            return n;
        }
    }

    public static InternalCall create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
        return new InternalCall(target, arguments, execution);
    }

    protected static class Specific extends InternalCallBase {
        @Child
        protected DirectCallNode callNode;
        private final PorcEClosure expectedTarget;
        @Child
        protected Expression notMatched;

        public Specific(final Expression target, final PorcEClosure expectedTarget, final Expression[] arguments, final Expression notMatched, final PorcEExecutionRef execution) {
            super(target, arguments, execution);
            this.expectedTarget = expectedTarget;
            this.notMatched = notMatched;
            this.callNode = Truffle.getRuntime().createDirectCallNode(expectedTarget.body);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            final PorcEClosure t = executeTargetClosure(frame);
            if (expectedTarget.body == t.body) {
                final Object[] argumentValues = buildArgumentValues(frame, t);
                return callNode.call(argumentValues);
            } else {
                return notMatched.execute(frame);
            }
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.POLYMORPHIC;
        }
        
        @Override
        public String toString() {
        	return "InternalCall.Specific(" + expectedTarget + ")";
        }
    }
    
    protected static class SelfTail extends InternalCallBase {
        @Child
        protected SelfTailCall callNode;
        private final RootNode expectedTarget;
        @Child
        protected Expression notMatched;

        public SelfTail(final Expression target, final RootNode expectedTarget, final Expression[] arguments, final Expression notMatched, final PorcEExecutionRef execution) {
            super(target, arguments, execution);
            this.expectedTarget = expectedTarget;
            this.notMatched = notMatched;
            this.callNode = SelfTailCall.create(arguments);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            final PorcEClosure t = executeTargetClosure(frame);
            if (expectedTarget == t.body.getRootNode()) {
                callNode.executeCall(frame, t.environment);
                return PorcEUnit.SINGLETON;
            } else {
                return notMatched.execute(frame);
            }
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.POLYMORPHIC;
        }
        
        @Override
        public String toString() {
        	return "InternalCall.SelfTail(" + expectedTarget + ")";
        }
    }

    protected static class Universal extends InternalCallBase {
        @Child
        protected IndirectCallNode callNode;

        public Universal(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
            super(target, arguments, execution);
            this.callNode = Truffle.getRuntime().createIndirectCallNode();
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            final PorcEClosure t = executeTargetClosure(frame);
            final Object[] argumentValues = buildArgumentValues(frame, t);
            return callNode.call(t.body, argumentValues);
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.POLYMORPHIC;
        }
    }

}
