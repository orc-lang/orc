package orc.run.porce;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class Argument extends Expression {
	public static Variable createVariable(FrameSlot s) {
		return new Variable(s);
	}

	public static Constant createConstant(Object v) {
		return new Constant(v);
	}

	public static PorcUnit createPorcUnit() {
		return new PorcUnit();
	}
}

class PorcUnit extends Argument {
	public Object execute(VirtualFrame frame) {
		return PorcEUnit.SINGLETON;
	}

	/*
	 * @Specialization public void executePorcEUnit(VirtualFrame frame) { }
	 */
}

class Constant extends Argument {
	private final Object value;

	public Constant(Object value) {
		this.value = value;
	}

	public Object execute(VirtualFrame frame) {
		return value;
	}
}

class Variable extends Argument {
	private final FrameSlot slot;

	public Variable(FrameSlot slot) {
		this.slot = slot;
	}

	public Object execute(VirtualFrame frame) {
		Object value = frame.getValue(slot);
		return value;
	}
}