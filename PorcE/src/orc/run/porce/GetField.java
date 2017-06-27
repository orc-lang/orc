package orc.run.porce;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import orc.Accessor;
import orc.AgressivelyInlined;
import orc.run.porce.runtime.PorcEExecution;
import orc.values.Field;

public class GetField extends Expression {
	protected final PorcEExecution execution;
	protected final Field field;
	
	@Child
	protected Expression object;

	public GetField(Expression object, Field field, PorcEExecution execution) {
		this.object = object;
		this.field = field;
		this.execution = execution;
	}

	@TruffleBoundary
	protected Accessor getAccessorWithBoundary(final Object t) {
		return execution.runtime().getAccessor(t, field);
	}

	@ExplodeLoop
	public Object execute(VirtualFrame frame) {
		final Object obj = object.execute(frame);
		
		Accessor accessor = getAccessorWithBoundary(obj);

			if (accessor instanceof AgressivelyInlined) {
				return accessor.get(obj);
			} else {
				return accessWithBoundary(accessor, obj);
			}
	}

	@TruffleBoundary(allowInlining = true)
	private static Object accessWithBoundary(final Accessor accessor, final Object obj) {
		return accessor.get(obj);
	}

	public static GetField create(Expression object, Field field, PorcEExecution execution) {
		return new GetField(object, field, execution);
	}
}
