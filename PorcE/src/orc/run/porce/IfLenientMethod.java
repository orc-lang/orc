package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.runtime.Utilities;

public class IfLenientMethod extends Expression {
	@Child
	protected Expression argument;
	@Child
	protected Expression left;
	@Child
	protected Expression right;

	public IfLenientMethod(Expression argument, Expression left, Expression right) {
		this.argument = argument;
		this.left = left;
		this.right = right;
	}

	public Object execute(VirtualFrame frame) {
		Object d = argument.execute(frame);
		//Logger.info(() -> "IfLenientMethod on " + d.toString() + "  " + Utilities.isDef(d) + "\n" + porcNode().toString().substring(0, 120));
		if(Utilities.isDef(d)) {
			return left.execute(frame);
		} else {
			return right.execute(frame);
		}
	}

	public static IfLenientMethod create(Expression argument, Expression left, Expression right) {
		return new IfLenientMethod(argument, left, right);
	}
}
