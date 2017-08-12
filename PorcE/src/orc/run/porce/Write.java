package orc.run.porce;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public class Write {
	private Write() {
	}

	public static class Local extends Expression {
		private final FrameSlot slot;
		@Child
		protected Expression value;

		protected Local(FrameSlot slot, Expression value) {
			this.slot = slot;
			this.value = value;
		}

		public Object execute(VirtualFrame frame) {
			frame.setObject(slot, value.execute(frame));
			return value;
		}

		public static Local create(FrameSlot slot, Expression value) {
			return new Local(slot, value);
		}
	}
}
