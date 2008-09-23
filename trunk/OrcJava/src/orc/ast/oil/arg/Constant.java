package orc.ast.oil.arg;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.ConstIntType;
import orc.type.Type;

public class Constant extends Arg { 
	
	public Object v;

	public Constant(Object v) {
		this.v = v;
	}

	@Override
	public <T> T resolve(Env<T> env) {
		return (T)v;
	}
	
	public String toString() {
		return "[" + v.toString() + "]";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(Env<Type> ctx) throws TypeException {
	
		if (v instanceof Integer) {
			return new ConstIntType((Integer)v);
		}
		if (v instanceof Number) {
			return Type.NUMBER;
		}
		else if (v instanceof String) {
			return Type.STRING;
		}
		else if (v instanceof Boolean) {
			return Type.BOOLEAN;
		}
		else {
			// TODO: Expand to cover arbitrary Java classes
			return Type.TOP;
		}
	}
}
