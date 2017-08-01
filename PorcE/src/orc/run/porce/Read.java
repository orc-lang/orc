package orc.run.porce;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public class Read {
	private Read() {
	}

	public static class Constant extends Expression {
		private final Object value;

		public Constant(Object value) {
			this.value = value;
			// TODO Auto-generated constructor stub
		}

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

		public Object execute(VirtualFrame frame) {
			// TODO: Change to getObject
			Object value = frame.getValue(slot);
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

		public Object execute(VirtualFrame frame) {
			Object value = ((Object[]) frame.getArguments()[0])[index];
			return value;
		}

		public static Closure create(int index) {
			assert index >= 0;
			return new Closure(index);
		}
	}
}
