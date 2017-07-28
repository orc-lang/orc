package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;

import orc.Accessor;
import orc.CaughtEvent;
import orc.error.runtime.HaltException;
import orc.run.porce.runtime.PorcEExecutionRef;
import orc.values.Field;

public class GetField extends Expression {
	protected final PorcEExecutionRef execution;
	protected final Field field;

	@Child
	protected Expression object;

	public GetField(Expression object, Field field, PorcEExecutionRef execution) {
		this.object = object;
		this.field = field;
		this.execution = execution;
	}

	public Object execute(VirtualFrame frame) {
		final Object obj = object.execute(frame);

		// FIXME: PERFORMANCE: Cache accessor and validate with accessor.canGet. With polymorphic cache.
		try {
			Accessor accessor = getAccessorWithBoundary(obj);
			return accessWithBoundary(accessor, obj);
		} catch (Exception e) {
			execution.get().notifyOrc(new CaughtEvent(e));
			throw HaltException.SINGLETON();
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

	public static GetField create(Expression object, Field field, PorcEExecutionRef execution) {
		return new GetField(object, field, execution);
	}
}
