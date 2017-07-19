package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import orc.run.porce.runtime.PorcEObject;
import orc.values.Field;

public class NewObject extends Expression {
	@Children
	protected final Expression[] expressions;
	private final Field[] fields;
	
	public NewObject(Field[] fields, Expression[] expressions) {
		this.fields = fields;
		this.expressions = expressions;
	}

	public Object execute(VirtualFrame frame) {
		return executePorcEObject(frame);
	}

	@ExplodeLoop
	public PorcEObject executePorcEObject(VirtualFrame frame) {
		Object[] values = new Object[expressions.length];
		for (int i = 0; i < expressions.length; i++) {
			values[i] = expressions[i].execute(frame);
		}
		return new PorcEObject(fields, values);
	}
	
	public static NewObject create(Field[] fields, Expression[] expressions) {
		return new NewObject(fields, expressions);
	}
}
