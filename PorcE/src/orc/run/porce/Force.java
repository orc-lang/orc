package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.FutureState;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.Join;
import orc.run.porce.runtime.Terminator;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;

public class Force {
	public static boolean isNonFuture(Object v) {
		return !(v instanceof orc.Future);
	}

	@NodeChild(value = "p", type = Expression.class)
	@NodeChild(value = "c", type = Expression.class)
	@NodeChild(value = "t", type = Expression.class)
	@NodeField(name = "nFutures", type = int.class)
	@NodeField(name = "execution", type = PorcEExecutionRef.class)
	public static class New extends Expression {
		@Specialization
		public Object run(int nFutures, PorcEExecutionRef execution, PorcEClosure p, Counter c, Terminator t) {
			Object[] values = new Object[nFutures + 1];
			return new Join(p, c, t, values, execution.get().runtime());
		}

		public static New create(Expression p, Expression c, Expression t, int nFutures, PorcEExecutionRef execution) {
			return ForceFactory.NewNodeGen.create(p, c, t, nFutures, execution);
		}
	}

	@NodeChild(value = "join", type = Expression.class)
	@NodeChild(value = "future", type = Expression.class)
	@NodeField(name = "index", type = int.class)
	@ImportStatic({Force.class})
	public static class Future extends Expression {
		@Specialization(guards = { "isNonFuture(future)" })
		public PorcEUnit nonFuture(int index, Join join, Object future) {
			join.set(index, future);
			return PorcEUnit.SINGLETON;
		}

		// TODO: PERFORMANCE: It may be worth playing with specializing by future states. Futures that are always bound may be common.
		@Specialization
		public PorcEUnit porceFuture(int index, Join join, orc.run.porce.runtime.Future future) {
			join.force(index, future);
			return PorcEUnit.SINGLETON;
		}

		@Specialization(replaces = { "porceFuture" })
		public PorcEUnit unknown(int index, Join join, orc.Future future) {
			join.force(index, future);
			return PorcEUnit.SINGLETON;
		}

		public static Future create(Expression join, Expression future, int index) {
			return ForceFactory.FutureNodeGen.create(join, future, index);
		}
	}

	@NodeChild(value = "join", type = Expression.class)
	@NodeField(name = "execution", type = PorcEExecutionRef.class)
	public static abstract class Finish extends Expression {
		volatile static int count = 0;
		@Child
		InternalArgArrayCallBase call = null;
		
		@Specialization(guards = { "join.isResolved()" })
		public PorcEUnit resolved(VirtualFrame frame, PorcEExecutionRef execution, Join join) {
			if (call == null) {
				CompilerDirectives.transferToInterpreterAndInvalidate();
				call = insert(InternalArgArrayCall.create(execution));
			}
			call.execute(frame, join.p(), join.values());
			/*
			count++;
			if (count > 10000 && count % 1000 == 0) {
				Logger.info(() -> "Finish count = " + count);
			}
			*/
			return PorcEUnit.SINGLETON;
		}

		@Specialization(guards = { "join.isHalted()" })
		public PorcEUnit halted(PorcEExecutionRef execution, Join join) {
			join.c().haltToken();
			return PorcEUnit.SINGLETON;
		}

		@Specialization(guards = { "join.isBlocked()" })
		public PorcEUnit blocked(PorcEExecutionRef execution, Join join) {
			join.finish();
			return PorcEUnit.SINGLETON;
		}

		public static Finish create(Expression join, PorcEExecutionRef execution) {
			return ForceFactory.FinishNodeGen.create(join, execution);
		}
	}
	
	@NodeChild(value = "p", type = Expression.class)
	@NodeChild(value = "c", type = Expression.class)
	@NodeChild(value = "t", type = Expression.class)
	@NodeChild(value = "future", type = Expression.class)
	@NodeField(name = "execution", type = PorcEExecutionRef.class)
	@ImportStatic({Force.class})
	public static abstract class SingleFuture extends Expression {
		volatile static int count = 0;
		
		@Child
		InternalArgArrayCallBase call = null;
		
		@Specialization(guards = { "isNonFuture(future)" })
		public Object nonFuture(VirtualFrame frame, PorcEExecutionRef execution, PorcEClosure p, Counter c, Terminator t, Object future) {
			initializeCall(execution);
			call.execute(frame, p, new Object[] { null, future });
			return PorcEUnit.SINGLETON;
		}
		
		@Specialization
		public PorcEUnit porceFuture(VirtualFrame frame, PorcEExecutionRef execution, PorcEClosure p, Counter c, Terminator t, orc.run.porce.runtime.Future future) {
	        Object state = future.getInternal();
	        if (state == orc.run.porce.runtime.FutureConstants.Halt) {
		        c.haltToken();
	        } else if (state == orc.run.porce.runtime.FutureConstants.Unbound) {
	        	future.read(new orc.run.porce.runtime.PCTFutureReader(p, c, t, execution.get().runtime()));
	        } else {
	        	initializeCall(execution);
	        	assert !(state instanceof orc.run.porce.runtime.FutureConstants.Sentinal);
				call.execute(frame, p, new Object[] { null, state });
	        }
			return PorcEUnit.SINGLETON;
		}
		
		@Specialization(replaces = { "porceFuture" })
		public Object future(VirtualFrame frame, PorcEExecutionRef execution, PorcEClosure p, Counter c, Terminator t, orc.Future future) {
	        FutureState state = future.get();
	        if (state instanceof orc.FutureState.Bound) {
				initializeCall(execution);
				call.execute(frame, p, new Object[] { null, ((orc.FutureState.Bound)state).value() });
	        } else if (state == orc.run.porce.runtime.FutureConstants.Orc_Stopped) {
		        c.haltToken();
	        } else if (state == orc.run.porce.runtime.FutureConstants.Orc_Unbound) {
	        	future.read(new orc.run.porce.runtime.PCTFutureReader(p, c, t, execution.get().runtime()));
	        }
			return PorcEUnit.SINGLETON;
		}

		private void initializeCall(PorcEExecutionRef execution) {
			if (call == null) {
				CompilerDirectives.transferToInterpreterAndInvalidate();
				call = insert(InternalArgArrayCall.create(execution));
			}
		}

		public static SingleFuture create(Expression p, Expression c, Expression t, Expression future, PorcEExecutionRef execution) {
			return ForceFactory.SingleFutureNodeGen.create(p, c, t, future, execution);
		}
	}
}
