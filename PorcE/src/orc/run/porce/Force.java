
package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Introspectable;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;

import orc.FutureState;
import orc.run.porce.call.Dispatch;
import orc.run.porce.call.InternalCPSDispatch;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Join;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.Terminator;
import static orc.run.porce.SpecializationConfiguration.*;

public class Force {
    public static boolean isNonFuture(final Object v) {
        return !(v instanceof orc.Future);
    }

    @NodeChild(value = "p", type = Expression.class)
    @NodeChild(value = "c", type = Expression.class)
    @NodeChild(value = "t", type = Expression.class)
    @NodeField(name = "nFutures", type = int.class)
    @NodeField(name = "execution", type = PorcEExecutionRef.class)
    public static class New extends Expression {
        @Specialization
        public Object run(final int nFutures, final PorcEExecutionRef execution, final PorcEClosure p, final Counter c, final Terminator t) {
            final Object[] values = new Object[nFutures + 1];
            return new Join(p, c, t, values, execution.get());
        }

        public static New create(final Expression p, final Expression c, final Expression t, final int nFutures, final PorcEExecutionRef execution) {
            return ForceFactory.NewNodeGen.create(p, c, t, nFutures, execution);
        }
    }

    @NodeChild(value = "join", type = Expression.class)
    @NodeChild(value = "future", type = Expression.class)
    @NodeField(name = "index", type = int.class)
    @ImportStatic({ Force.class })
    public static class Future extends Expression {
        @Specialization(guards = { "isNonFuture(future)" })
        public PorcEUnit nonFuture(final int index, final Join join, final Object future) {
            join.set(index, future);
            return PorcEUnit.SINGLETON;
        }

        // TODO: PERFORMANCE: It may be worth playing with specializing by
        // future states. Futures that are always bound may be common.
        // This would only be worth it if multi-value force is called
        // frequently.
        @Specialization
        public PorcEUnit porceFuture(final int index, final Join join, final orc.run.porce.runtime.Future future) {
            join.force(index, future);
            return PorcEUnit.SINGLETON;
        }

        @Specialization(replaces = { "porceFuture" })
        public PorcEUnit unknown(final int index, final Join join, final orc.Future future) {
            join.force(index, future);
            return PorcEUnit.SINGLETON;
        }

        public static Future create(final Expression join, final Expression future, final int index) {
            return ForceFactory.FutureNodeGen.create(join, future, index);
        }
    }

    @NodeChild(value = "join", type = Expression.class)
    @NodeField(name = "execution", type = PorcEExecutionRef.class)
    @Introspectable
    @ImportStatic(SpecializationConfiguration.class)
    public static abstract class Finish extends Expression {
        @Child
        Dispatch call = null;

        @Specialization(guards = { "InlineForceResolved", "join.isResolved()" })
        public PorcEUnit resolved(final VirtualFrame frame, final PorcEExecutionRef execution, final Join join) {
        	if (call == null) {
    			CompilerDirectives.transferToInterpreterAndInvalidate();
	        	computeAtomicallyIfNull(() -> call, (v) -> call = v, () -> {
	        		Dispatch n = insert(InternalCPSDispatch.create(true, execution, isTail));
	        		n.setTail(isTail);
	        		return n;
	        	});
        	}
        	
            if (call instanceof InternalCPSDispatch) {
            	((InternalCPSDispatch)call).executeDispatchWithEnvironment(frame, join.p(), join.values());
            } else {
                Object[] values = join.values();
                Object[] valuesWithoutPrefix = new Object[values.length - 1];
                System.arraycopy(values, 1, valuesWithoutPrefix, 0, valuesWithoutPrefix.length);
                call.executeDispatch(frame, join.p(), valuesWithoutPrefix);
            }
            return PorcEUnit.SINGLETON;
        }

        @Specialization(guards = { "InlineForceHalted", "join.isHalted()" })
        public PorcEUnit halted(final PorcEExecutionRef execution, final Join join) {
            join.c().haltToken();
            return PorcEUnit.SINGLETON;
        }

        @Specialization(guards = { "join.isBlocked()" })
        public PorcEUnit blocked(final PorcEExecutionRef execution, final Join join) {
            join.finishBlocked();
            return PorcEUnit.SINGLETON;
        }
        
        @Specialization(guards = { "!InlineForceResolved || !InlineForceHalted" })
        public PorcEUnit fallback(final PorcEExecutionRef execution, final Join join) {
            join.finish();
            return PorcEUnit.SINGLETON;
        }

        public static Finish create(final Expression join, final PorcEExecutionRef execution) {
            return ForceFactory.FinishNodeGen.create(join, execution);
        }
    }

    @NodeChild(value = "p", type = Expression.class)
    @NodeChild(value = "c", type = Expression.class)
    @NodeChild(value = "t", type = Expression.class)
    @NodeChild(value = "future", type = Expression.class)
    @NodeField(name = "execution", type = PorcEExecutionRef.class)
    @ImportStatic({ Force.class })
    public static abstract class SingleFuture extends Expression {
        @Child
        protected Dispatch call = null;
        private final ResettableBranchProfile boundFuture = ResettableBranchProfile.create();
        private final ResettableBranchProfile unboundFuture = ResettableBranchProfile.create();
        private final ConditionProfile orcFuture = ConditionProfile.createBinaryProfile();
        private final ConditionProfile porcEFuture = ConditionProfile.createBinaryProfile();
        private final ConditionProfile nonFuture = ConditionProfile.createBinaryProfile();
        

        @SuppressWarnings("serial")
        private static final class ValueAvailable extends ControlFlowException {
            public final Object value;

            public ValueAvailable(final Object value) {
                this.value = value;
            }
        }

        @Specialization
        public Object run(final VirtualFrame frame, final PorcEExecutionRef execution, final PorcEClosure p, final Counter c, final Terminator t, final Object future) {
            try {
                if (nonFuture.profile(isNonFuture(future))) {
                    throw new ValueAvailable(future);
                } else if (porcEFuture.profile(future instanceof orc.run.porce.runtime.Future)) {
                    final Object state = ((orc.run.porce.runtime.Future) future).getInternal();
                    if (InlineForceResolved && !(state instanceof orc.run.porce.runtime.FutureConstants.Sentinel)) {
                    	if (boundFuture.enter()) {
                        	unboundFuture.reset();
                        }
                        throw new ValueAvailable(state);
                    } else {
                        unboundFuture.enter();
                        if (InlineForceHalted && state == orc.run.porce.runtime.FutureConstants.Halt) {
                        	// TODO: PERFORMANCE: This cannot inline the halt continuation. Using a version of HaltToken would allow that.
                            c.haltToken();
                        } else {
                            ((orc.run.porce.runtime.Future) future).read(new orc.run.porce.runtime.SingleFutureReader(p, c, t, execution.get()));
                        }
                    }
                } else if (orcFuture.profile(future instanceof orc.Future)) {
                    final FutureState state = ((orc.Future) future).get();
                    if (InlineForceResolved && state instanceof orc.FutureState.Bound) {
                    	if (boundFuture.enter()) {
                        	unboundFuture.reset();
                        }
                        throw new ValueAvailable(((orc.FutureState.Bound) state).value());
                    } else {
                        unboundFuture.enter();
                    	if (InlineForceHalted && state == orc.run.porce.runtime.FutureConstants.Orc_Stopped) {
                        	// TODO: PERFORMANCE: This cannot inline the halt continuation. Using a version of HaltToken would allow that.
	                        c.haltToken();
	                    } else {
	                        ((orc.Future) future).read(new orc.run.porce.runtime.SingleFutureReader(p, c, t, execution.get()));
	                    }
                    }
                } else {
                    InternalPorcEError.unreachable(this);
                }
            } catch (final ValueAvailable e) {
                initializeCall(execution);
                	
                if (call instanceof InternalCPSDispatch) {
                	((InternalCPSDispatch)call).executeDispatchWithEnvironment(frame, p, new Object[] { null, e.value });
                } else {
                	call.executeDispatch(frame, p, new Object[] { e.value });
                }
            }

            return PorcEUnit.SINGLETON;
        }

        /**
         * 
         * @param execution
         * @return true if this did transferToInterpreterAndInvalidate.
         */
        private boolean initializeCall(final PorcEExecutionRef execution) {
			if (call == null) {
				CompilerDirectives.transferToInterpreterAndInvalidate();
				computeAtomicallyIfNull(() -> call, (v) -> call = v, () -> {
					Dispatch n = insert(InternalCPSDispatch.create(true, execution, isTail));
					n.setTail(isTail);
					return n;
				});
				return true;
			}
			return false;
        }

        public static SingleFuture create(final Expression p, final Expression c, final Expression t, final Expression future, final PorcEExecutionRef execution) {
            return ForceFactory.SingleFutureNodeGen.create(p, c, t, future, execution);
        }
    }
}
