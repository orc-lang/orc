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

	public NewCounter(PorcERuntime runtime, Expression parent) {
		this.runtime = runtime;
		this.parent = parent;
	}

	public Object execute(VirtualFrame frame) {
		return executeCounter(frame);
	}

	public abstract Counter executeCounter(VirtualFrame frame);

	public final static class Simple extends NewCounter {
		@Child
		protected Expression haltContinuation;

		public Simple(PorcERuntime runtime, Expression parent, Expression haltContinuation) {
			super(runtime, parent);
			this.haltContinuation = haltContinuation;
		}

		public Counter executeCounter(VirtualFrame frame) {
			try {
				return new CounterNested(runtime, parent.executeCounter(frame), haltContinuation.executePorcEClosure(frame));
			} catch (UnexpectedResultException e) {
				throw InternalPorcEError.typeError(this, e);
			}
		}

		public static NewCounter create(PorcERuntime runtime, Expression parent, Expression haltContinuation) {
			return new Simple(runtime, parent, haltContinuation);
		}
	}

	public final static class Service extends NewCounter {
		@Child
		protected Expression parentContaining;
		@Child
		protected Expression terminator;

		public Service(PorcERuntime runtime, Expression parentCalling, Expression parentContaining, Expression terminator) {
			super(runtime, parentCalling);
			this.parentContaining = parentContaining;
			this.terminator = terminator;
		}

		public Counter executeCounter(VirtualFrame frame) {
			try {
				return new CounterService(runtime, parent.executeCounter(frame), parentContaining.executeCounter(frame), terminator.executeTerminator(frame));
			} catch (UnexpectedResultException e) {
				throw InternalPorcEError.typeError(this, e);
			}
		}		
		
		public static NewCounter create(PorcERuntime runtime, Expression parentCalling, Expression parentContaining, Expression haltContinuation) {
			return new Service(runtime, parentCalling, parentContaining, haltContinuation);
		}
	}

	public final static class Terminator extends NewCounter {
		@Child
		protected Expression terminator;

		public Terminator(PorcERuntime runtime, Expression parent, Expression terminator) {
			super(runtime, parent);
			this.terminator = terminator;
		}

		public Counter executeCounter(VirtualFrame frame) {
			try {
				return new CounterTerminator(runtime, parent.executeCounter(frame), terminator.executeTerminator(frame));
			} catch (UnexpectedResultException e) {
				throw InternalPorcEError.typeError(this, e);
			}
		}

		public static NewCounter create(PorcERuntime runtime, Expression parent, Expression haltContinuation) {
			return new Terminator(runtime, parent, haltContinuation);
		}
	}
}
