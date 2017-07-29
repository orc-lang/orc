package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public class Read {
	private Read() {
	}

	@NodeField(name = "value", type = Object.class)
	public static class Constant extends Expression {
		@Specialization
		public Object execute(Object value) {
			return value;
		}

		public static Constant create(Object v) {
			return ReadFactory.ConstantNodeGen.create(v);
		}
	}

	@NodeField(name = "slot", type = FrameSlot.class)
	public static class Local extends Expression {
		@Specialization
		public Object execute(VirtualFrame frame, FrameSlot slot) {
			Object value = frame.getValue(slot);
			return value;
		}

		public static Local create(FrameSlot slot) {
			assert slot != null;
			return ReadFactory.LocalNodeGen.create(slot);
		}
	}

	@NodeField(name = "index", type = int.class)
	public static class Argument extends Expression {
		@Specialization
		public Object execute(VirtualFrame frame, int index) {
			Object value = frame.getArguments()[index];
			return value;
		}

		public static Argument create(int index) {
			assert index >= 0;
			return ReadFactory.ArgumentNodeGen.create(index + 1);
		}
	}

	@NodeField(name = "index", type = int.class)
	public static class Closure extends Expression {
		@Specialization
		public Object execute(VirtualFrame frame, int index) {
			Object value = ((Object[]) frame.getArguments()[0])[index];
			return value;
		}

		public static Closure create(int index) {
			assert index >= 0;
			return ReadFactory.ClosureNodeGen.create(index);
		}
	}
}
