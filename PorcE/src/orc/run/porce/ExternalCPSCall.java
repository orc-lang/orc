package orc.run.porce;

import java.util.concurrent.locks.Lock;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import orc.CaughtEvent;
import orc.DirectInvoker;
import orc.ErrorInvoker;
import orc.Invoker;
import orc.error.runtime.ExceptionHaltException;
import orc.error.runtime.HaltException;
import orc.run.porce.InternalCall.Specific;
import orc.run.porce.InternalCall.Universal;
import orc.run.porce.runtime.Counter;
import orc.run.porce.runtime.PCTHandle;
import orc.run.porce.runtime.PorcEClosure;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.run.porce.runtime.PorcERuntime;
import orc.run.porce.runtime.Terminator;

class ExternalCPSCallBase extends CallBase {
	public ExternalCPSCallBase(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		super(target, arguments, execution);
	}

	protected Object[] buildArgumentValues(VirtualFrame frame) {
		final Object[] argumentValues = new Object[arguments.length - 3];
		executeArguments(argumentValues, 0, 3, frame);
		return argumentValues;
	}

	protected PorcEClosure executeP(VirtualFrame frame) {
		try {
			return arguments[0].executePorcEClosure(frame);
		} catch (UnexpectedResultException e) {
			throw InternalPorcEError.typeError(this, e);
		}
	}

	protected Counter executeC(VirtualFrame frame) {
		try {
			return arguments[1].executeCounter(frame);
		} catch (UnexpectedResultException e) {
			throw InternalPorcEError.typeError(this, e);
		}
	}

	protected Terminator executeT(VirtualFrame frame) {
		try {
			return arguments[2].executeTerminator(frame);
		} catch (UnexpectedResultException e) {
			throw InternalPorcEError.typeError(this, e);
		}
	}

	protected Invoker getInvokerWithBoundary(final Object t, final Object[] argumentValues) {
		return getInvokerWithBoundary(getRuntime(), t, argumentValues);
	}

	@TruffleBoundary(allowInlining = true)
	protected static Invoker getInvokerWithBoundary(final PorcERuntime runtime, final Object t,
			final Object[] argumentValues) {
		return runtime.getInvoker(t, argumentValues);
	}

	@TruffleBoundary(allowInlining = true)
	protected static boolean canInvokeWithBoundary(final Invoker invoker, final Object t,
			final Object[] argumentValues) {
		return invoker.canInvoke(t, argumentValues);
	}

	@TruffleBoundary(allowInlining = true)
	protected static void invokeWithBoundary(final Invoker invoker, final PCTHandle handle, final Object t,
			final Object[] argumentValues) {
		invoker.invoke(handle, t, argumentValues);
	}
}

public class ExternalCPSCall extends ExternalCPSCallBase {
	private int cacheSize = 0;
	private static int cacheMaxSize = 4;

	public ExternalCPSCall(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		super(target, arguments, execution);
	}

	public void executePorcEUnit(VirtualFrame frame) {
		CompilerDirectives.transferToInterpreterAndInvalidate();

		Object t = executeTargetObject(frame);
		Object[] argumentValues = buildArgumentValues(frame);
		CallBase n;

		try {
			Invoker invoker = getInvokerWithBoundary(t, argumentValues);

			Lock lock = getLock();
			lock.lock();
			try {
				if (!(invoker instanceof ErrorInvoker) && cacheSize < cacheMaxSize) {
					cacheSize++;
					n = new Specific((Expression) target.copy(), invoker, copyExpressionArray(arguments),
							(CallBase) this.copy(), execution);
					replace(n, "Speculate on target closure.");
				} else {
					n = replaceWithUniversal();
				}
			} finally {
				lock.unlock();
			}
		} catch (Exception e) {
			execution.get().notifyOrc(new CaughtEvent(e));
			replaceWithUniversal();
			throw HaltException.SINGLETON();
		}
		n.executePorcEUnit(frame);
	}

	private CallBase replaceWithUniversal() {
		CallBase n = new Universal(target, arguments, execution);
		findCacheRoot(this).replace(n,
				"Invoker cache too large or error getting invoker. Falling back to universal invocation.");
		return n;
	}

	private static CallBase findCacheRoot(CallBase n) {
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

	public static ExternalCPSCall create(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
		return new ExternalCPSCall(target, arguments, execution);
	}

	public static class Specific extends ExternalCPSCallBase {
		private final Invoker invoker;

		@Child
		protected Expression notMatched;

		public Specific(Expression target, Invoker invoker, Expression[] arguments, Expression notMatched,
				PorcEExecutionRef execution) {
			super(target, arguments, execution);
			this.invoker = invoker;
			this.notMatched = notMatched;
		}

		public void executePorcEUnit(VirtualFrame frame) {
			Object t = executeTargetObject(frame);
			Object[] argumentValues = buildArgumentValues(frame);

			if (canInvokeWithBoundary(invoker, t, argumentValues)) {
				PorcEClosure pub = executeP(frame);
				Counter counter = executeC(frame);
				Terminator term = executeT(frame);

				// Token: Passed to handle from arguments.
				final PCTHandle handle = new PCTHandle(execution.get(), pub, counter, term);

				try {
					invokeWithBoundary(invoker, handle, t, argumentValues);
				} catch (ExceptionHaltException e) {
					execution.get().notifyOrc(new CaughtEvent(e.getCause()));
				} catch (HaltException e) {
				} catch (Exception e) {
					execution.get().notifyOrc(new CaughtEvent(e));
					throw HaltException.SINGLETON();
				}
			} else {
				notMatched.execute(frame);
			}
		}

		@Override
		public NodeCost getCost() {
			return NodeCost.POLYMORPHIC;
		}
	}

	public static class Universal extends ExternalCPSCallBase {
		public Universal(Expression target, Expression[] arguments, PorcEExecutionRef execution) {
			super(target, arguments, execution);
		}

		public void executePorcEUnit(VirtualFrame frame) {
			Object t = executeTargetObject(frame);
			Object[] argumentValues = buildArgumentValues(frame);

			PorcEClosure pub = executeP(frame);
			Counter counter = executeC(frame);
			Terminator term = executeT(frame);

			// Token: Passed to handle from arguments.
			final PCTHandle handle = new PCTHandle(execution.get(), pub, counter, term);

			try {
				Invoker invoker = getInvokerWithBoundary(t, argumentValues);
				invokeWithBoundary(invoker, handle, t, argumentValues);
			} catch (ExceptionHaltException e) {
				execution.get().notifyOrc(new CaughtEvent(e.getCause()));
			} catch (HaltException e) {
			} catch (Exception e) {
				execution.get().notifyOrc(new CaughtEvent(e));
				throw HaltException.SINGLETON();
			}
		}

		@Override
		public NodeCost getCost() {
			return NodeCost.MEGAMORPHIC;
		}
	}
}
