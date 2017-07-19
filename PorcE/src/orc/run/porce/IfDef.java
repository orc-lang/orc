package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

import orc.run.porce.runtime.Utilities;

public class IfDef extends Expression {
	@Child
	protected Expression argument;
	@Child
	protected Expression left;
	@Child
	protected Expression right;

	public IfDef(Expression argument, Expression left, Expression right) {
		this.argument = argument;
		this.left = left;
		this.right = right;
	}

	public Object execute(VirtualFrame frame) {
		if(Utilities.isDef(argument.execute(frame))) {
			return left.execute(frame);
		} else {
			return right.execute(frame);
		}
	}

	public static IfDef create(Expression argument, Expression left, Expression right) {
		return new IfDef(argument, left, right);
	}
}