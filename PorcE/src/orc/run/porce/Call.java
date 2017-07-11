package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.AgressivelyInlined;
import orc.CaughtEvent;
import orc.DirectInvoker;
import orc.Invoker;
import orc.error.runtime.ExceptionHaltException;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PCTHandle;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecution;
import orc.run.porce.runtime.Terminator;

public abstract class Call extends Expression {
	// FIXME: Split this into multiple different node types for only internal
	// and only external invocations. Then have wrapper nodes which dispatch to
	// internal and external handling as needed. This would allow us to easily
	// separate caching strategies of the different invocation types.
	
	// TODO: PERFORMANCE: Add profiling to optimize the internal/external dispatch.

	// FIXME: PERFORMANCE: Implement polymorphic inline cache for internal calls. See
	// http://cesquivias.github.io/blog/2015/01/08/writing-a-language-in-truffle-part-3-making-my-language-much-faster/#making-function-calls-faster

	// FIXME: PERFORMANCE: Implement polymorphic inline cache for invokers check with canInvoke.

	@Child
	protected Expression target;
	@Children
	protected final Expression[] arguments;
	@Child
	protected IndirectCallNode callNode;

	protected final PorcEExecution execution;

	public Call(Expression target, Expression[] arguments, PorcEExecution execution) {
		this.target = target;
		this.arguments = arguments;
		this.execution = execution;
		this.callNode = Truffle.getRuntime().createIndirectCallNode();
	}

	@ExplodeLoop
	public Object callInternal(VirtualFrame frame) {
		PorcEClosure t;
		try {
			t = target.executePorcEClosure(frame);
		} catch (UnexpectedResultException e) {
			throw InternalPorcEError.typeError(this, e);
		}
		Object[] argumentValues = new Object[arguments.length + 1];
		argumentValues[0] = t.capturedValues;
		for (int i = 0; i < arguments.length; i++) {
			argumentValues[i + 1] = arguments[i].execute(frame);
		}
		return callNode.call(t.body, argumentValues);
	}

	@TruffleBoundary
	protected Invoker getInvokerWithBoundary(final Object t, final Object[] argumentValues) {
		return execution.runtime().getInvoker(t, argumentValues);
	}

	public static class InternalOnly extends Call {
		public InternalOnly(Expression target, Expression[] arguments, PorcEExecution execution) {
			super(target, arguments, execution);
		}

		public Object execute(VirtualFrame frame) {
			return callInternal(frame);
		}

		public static Call create(Expression target, Expression[] arguments, PorcEExecution execution) {
			return new InternalOnly(target, arguments, execution);
		}
	}

	public static class CPS extends Call {
		public CPS(Expression target, Expression[] arguments, PorcEExecution execution) {
			super(target, arguments, execution);
		}

		@ExplodeLoop
		public void executePorcEUnit(VirtualFrame frame) {
			final Object t = target.execute(frame);
			if (t instanceof PorcEClosure) {
				callInternal(frame);
			} else {
				final int realArgsLen = arguments.length - 3;
				final Object[] argumentValues = new Object[realArgsLen];
				PorcEClosure pub;
				Terminator term;
				Counter counter;
				try {
					pub = arguments[0].executePorcEClosure(frame);
					counter = arguments[1].executeCounter(frame);
					term = arguments[2].executeTerminator(frame);
				} catch (UnexpectedResultException e) {
					// FIXME: What's the correct exception here?
					throw new UnsupportedSpecializationException(this,
							new Node[] { arguments[0], arguments[1], arguments[2] }, e, e, e);
				}
				for (int i = 0; i < realArgsLen; i++) {
					argumentValues[i] = arguments[i + 3].execute(frame);
				}
				Invoker invoker = getInvokerWithBoundary(t, argumentValues);
				final PCTHandle handle = new PCTHandle(execution, pub, counter, term);

				counter.prepareSpawn();
				if (invoker instanceof AgressivelyInlined) {
					invoker.invoke(handle, t, argumentValues);
				} else {
					invokeWithBoundary(invoker, handle, t, argumentValues);
				}
			}
		}

		@TruffleBoundary(allowInlining = true)
		private static void invokeWithBoundary(final Invoker invoker, final PCTHandle handle, final Object t,
				final Object[] argumentValues) {
			invoker.invoke(handle, t, argumentValues);
		}

		public static Call create(Expression target, Expression[] arguments, PorcEExecution execution) {
			return new CPS(target, arguments, execution);
		}
	}

	public static class Direct extends Call {
		public Direct(Expression target, Expression[] arguments, PorcEExecution execution) {
			super(target, arguments, execution);
		}

		@ExplodeLoop
		public Object execute(VirtualFrame frame) {
			final Object t = target.execute(frame);
			if (t instanceof PorcEClosure) {
				return callInternal(frame);
			} else {
				final Object[] argumentValues = new Object[arguments.length];
				for (int i = 0; i < arguments.length; i++) {
					argumentValues[i] = arguments[i].execute(frame);
				}
				DirectInvoker invoker = (DirectInvoker) getInvokerWithBoundary(t, argumentValues);

				try {
					if (invoker instanceof AgressivelyInlined) {
						return invoker.invokeDirect(t, argumentValues);
					} else {
						return invokeWithBoundary(invoker, t, argumentValues);
					}
				} catch (ExceptionHaltException e) {
					execution.notifyOrc(new CaughtEvent(e.getCause()));
					throw HaltException.SINGLETON();
				} catch (HaltException e) {
					throw e;
				} catch (Exception e) {
					execution.notifyOrc(new CaughtEvent(e.getCause()));
					throw HaltException.SINGLETON();
				}
			}
		}

		@TruffleBoundary(allowInlining = true)
		private static Object invokeWithBoundary(final DirectInvoker invoker, final Object t,
				final Object[] argumentValues) {
			return invoker.invokeDirect(t, argumentValues);
		}

		public static Call create(Expression target, Expression[] arguments, PorcEExecution execution) {
			return new Direct(target, arguments, execution);
		}
	}
}
