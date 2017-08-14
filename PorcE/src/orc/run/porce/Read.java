package orc.run.porce;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;

public class Read {
	private Read() {
	}

	public static class Constant extends Expression {
		private final Object value;

		public Constant(Object value) {
			this.value = value;
		}

		@Override
        public Object execute(VirtualFrame frame) {
			return value;
		}

		public static Constant create(Object v) {
			return new Constant(v);
		}
	}

	public static class Local extends Expression {
		private final FrameSlot slot;

		public Local(FrameSlot slot) {
			this.slot = slot;
		}

		@Override
        public Object execute(VirtualFrame frame) {
			Object value;
			try {
				value = frame.getObject(slot);
			} catch (FrameSlotTypeException e) {
				throw InternalPorcEError.typeError(this, e);
			}
			return value;
		}

		public static Local create(FrameSlot slot) {
			assert slot != null;
			return new Local(slot);
		}
	}

	public static class Argument extends Expression {
		private final int index;

		public Argument(int index) {
			this.index = index;
		}

		@Override
        public Object execute(VirtualFrame frame) {
			Object value = frame.getArguments()[index];
			return value;
		}

		public static Argument create(int index) {
			assert index >= 0;
			return new Argument(index + 1);
		}
	}

	public static class Closure extends Expression {
		private final int index;

		public Closure(int index) {
			this.index = index;
		}

		@Override
        public Object execute(VirtualFrame frame) {
			// TODO: PERFORMANCE: Storing the closure in a frame slot would save an indexed load per node.
			Object value = ((Object[]) frame.getArguments()[0])[index];
			return value;
		}

		public static Closure create(int index) {
			assert index >= 0;
			return new Closure(index);
		}
	}
}
