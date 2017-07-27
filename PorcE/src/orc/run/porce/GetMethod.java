package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

import orc.Accessor;
import orc.CaughtEvent;
import orc.ErrorAccessor;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.values.Field;

public class GetMethod extends Expression {
	protected final PorcEExecutionRef execution;
	protected final Field field = Field.create("apply");

	@Child
	protected Expression object;

	public GetMethod(Expression object, PorcEExecutionRef execution) {
		this.object = object;
		this.execution = execution;
	}

	public Object execute(VirtualFrame frame) {
		final Object obj = object.execute(frame);

		Accessor accessor = getAccessorWithBoundary(obj);
		// FIXME: PERFORMANCE: Cache accessor and validate with accessor.canGet. With polymorphic cache.

		if (accessor instanceof ErrorAccessor) {
			return obj;
		} else {
			try {
				return accessWithBoundary(accessor, obj);
			} catch (Exception e) {
				execution.get().notifyOrc(new CaughtEvent(e));
				throw HaltException.SINGLETON();
			}
		}
	}

	@TruffleBoundary
	protected Accessor getAccessorWithBoundary(final Object t) {
		return execution.get().runtime().getAccessor(t, field);
	}

	@TruffleBoundary(allowInlining = true)
	private static Object accessWithBoundary(final Accessor accessor, final Object obj) {
		return accessor.get(obj);
	}

	public static GetMethod create(Expression object, PorcEExecutionRef execution) {
		return new GetMethod(object, execution);
	}
}
