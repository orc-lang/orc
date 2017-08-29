
package orc.run.porce;

import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.profiles.BranchProfile;
import orc.CaughtEvent;
import orc.DirectInvoker;
import orc.ErrorInvoker;
import orc.Invoker;
import orc.error.runtime.ExceptionHaltException;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;

class ExternalDirectCallBase extends CallBase {
    @CompilerDirectives.CompilationFinal(dimensions = 1)
    protected final BranchProfile[] exceptionProfiles = new BranchProfile[] { BranchProfile.create(), BranchProfile.create(), BranchProfile.create() };

    public ExternalDirectCallBase(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
        super(target, arguments, execution);
    }

    protected Object[] buildArgumentValues(final VirtualFrame frame) {
        final Object[] argumentValues = new Object[arguments.length];
        executeArguments(argumentValues, 0, 0, frame);
        return argumentValues;
    }

    protected DirectInvoker getInvokerWithBoundary(final Object t, final Object[] argumentValues) {
        return (DirectInvoker) getInvokerWithBoundary(getRuntime(), t, argumentValues);
    }

    @TruffleBoundary(allowInlining = true)
    protected static Invoker getInvokerWithBoundary(final PorcERuntime runtime, final Object t, final Object[] argumentValues) {
        return runtime.getInvoker(t, argumentValues);
    }

    @TruffleBoundary(allowInlining = true)
    protected static boolean canInvokeWithBoundary(final Invoker invoker, final Object t, final Object[] argumentValues) {
        return invoker.canInvoke(t, argumentValues);
    }

    @TruffleBoundary(allowInlining = true, throwsControlFlowException = true)
    protected static Object invokeDirectWithBoundary(final DirectInvoker invoker, final Object t, final Object[] argumentValues) {
        return invoker.invokeDirect(t, argumentValues);
    }
}

public class ExternalDirectCall extends ExternalDirectCallBase {
    private int cacheSize = 0;
    private static int cacheMaxSize = 4;

    public ExternalDirectCall(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
        super(target, arguments, execution);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final Object t = executeTargetObject(frame);
        final Object[] argumentValues = buildArgumentValues(frame);
        CallBase n;

        try {
            final DirectInvoker invoker = getInvokerWithBoundary(t, argumentValues);

            final Lock lock = getLock();
            lock.lock();
            try {
                if (!(invoker instanceof ErrorInvoker) && cacheSize < cacheMaxSize) {
                    cacheSize++;
                    n = new Specific((Expression) target.copy(), invoker, copyExpressionArray(arguments), (CallBase) this.copy(), execution);
                    replace(n, "ExternalDirectCall: Speculate on target invoker.");
                } else {
                    n = replaceWithUniversal();
                }
            } finally {
                lock.unlock();
            }
        } catch (final Exception e) {
            execution.get().notifyOrcWithBoundary(new CaughtEvent(e));
            replaceWithUniversal();
            throw HaltException.SINGLETON();
        }
        return n.execute(frame);
    }

    private CallBase replaceWithUniversal() {
        final CallBase n = new Universal(target, arguments, execution);
        findCacheRoot(this).replace(n, "Invoker cache too large or error getting invoker. Falling back to universal invocation.");
        return n;
    }

    private static CallBase findCacheRoot(final CallBase n) {
        if (n.getParent() instanceof Specific) {
            return findCacheRoot((Specific) n.getParent());
        } else {
            return n;
        }
    }

    @Override
    public NodeCost getCost() {
        return NodeCost.UNINITIALIZED;
    }

    public static ExternalDirectCall create(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
        return new ExternalDirectCall(target, arguments, execution);
    }

    public static class Specific extends ExternalDirectCallBase {
        private final DirectInvoker invoker;

        @Child
        protected Expression notMatched;

        public Specific(final Expression target, final DirectInvoker invoker, final Expression[] arguments, final Expression notMatched, final PorcEExecutionRef execution) {
            super(target, arguments, execution);
            this.invoker = invoker;
            this.notMatched = notMatched;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            final Object t = executeTargetObject(frame);
            final Object[] argumentValues = buildArgumentValues(frame);

            if (canInvokeWithBoundary(invoker, t, argumentValues)) {
                Object r;
                try {
                    r = invokeDirectWithBoundary(invoker, t, argumentValues);
                } catch (final ExceptionHaltException e) {
                    exceptionProfiles[0].enter();
                    execution.get().notifyOrcWithBoundary(new CaughtEvent(e.getCause()));
                    throw HaltException.SINGLETON();
                } catch (final HaltException e) {
                    exceptionProfiles[1].enter();
                    throw e;
                } catch (final Exception e) {
                    exceptionProfiles[2].enter();
                    execution.get().notifyOrcWithBoundary(new CaughtEvent(e));
                    throw HaltException.SINGLETON();
                }
                return r;
            } else {
                return notMatched.execute(frame);
            }
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.POLYMORPHIC;
        }
    }

    public static class Universal extends ExternalDirectCallBase {
        public Universal(final Expression target, final Expression[] arguments, final PorcEExecutionRef execution) {
            super(target, arguments, execution);
        }

        @Override
        @ExplodeLoop
        public Object execute(final VirtualFrame frame) {
            final Object t = executeTargetObject(frame);
            final Object[] argumentValues = buildArgumentValues(frame);

            try {
                final DirectInvoker invoker = getInvokerWithBoundary(t, argumentValues);
                return invokeDirectWithBoundary(invoker, t, argumentValues);
            } catch (final ExceptionHaltException e) {
                exceptionProfiles[0].enter();
                execution.get().notifyOrcWithBoundary(new CaughtEvent(e.getCause()));
                throw HaltException.SINGLETON();
            } catch (final HaltException e) {
                exceptionProfiles[1].enter();
                throw e;
            } catch (final Exception e) {
                exceptionProfiles[2].enter();
                execution.get().notifyOrcWithBoundary(new CaughtEvent(e));
                throw HaltException.SINGLETON();
            }
        }

        @Override
        public NodeCost getCost() {
            return NodeCost.POLYMORPHIC;
        }
    }
}
