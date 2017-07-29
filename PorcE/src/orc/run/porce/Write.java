package orc.run.porce;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public class Write {
	private Write() {
	}

	@NodeField(name = "slot", type = FrameSlot.class)
	@NodeChild("value")
	public static class Local extends Expression {
		@Specialization
		public Object execute(VirtualFrame frame, FrameSlot slot, Object value) {
			frame.setObject(slot, value);
			return value;
		}

		public static Local create(FrameSlot slot, Expression value) {
			return WriteFactory.LocalNodeGen.create(value, slot);
		}
	}
}
