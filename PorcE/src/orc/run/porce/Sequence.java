package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

public class Sequence extends Expression {
	@Children
	protected final Expression[] exprs;

	public Sequence(Expression[] exprs) {
		this.exprs = exprs;
	}

	@ExplodeLoop
	public Object execute(VirtualFrame frame) {
		for (int i = 0; i < exprs.length - 1; i++) {
			Expression expr = exprs[i];
			expr.executePorcEUnit(frame);
		}
		return exprs[exprs.length - 1].execute(frame);
	}

	public static Sequence create(Expression[] exprs) {
		return new Sequence(exprs);
	}
}
