package orc.ast.oil.expression.argument;

import java.math.BigInteger;
import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.ground.ConstIntType;
import xtc.util.Utilities;

public class Constant extends Argument { 
	
	public Object v;

	public Constant(Object v) {
		this.v = v;
	}

	@Override
	public Object resolve(Env<Object> env) {
		return v;
	}
	
	public String toString() {
		if (v == null) {
			return "null";
		} else if (v instanceof String) {
			return '"' + Utilities.escape((String)v, Utilities.JAVA_ESCAPES) + '"';
		} else {
			return v.toString();
		}
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public Type typesynth(TypingContext ctx) throws TypeException {
		if (v == null) {
			return Type.BOT;
		} else if (v instanceof Integer) {
			return new ConstIntType((Integer)v);
		} else if (v instanceof BigInteger) {
			return Type.INTEGER;
		} else if (v instanceof Number) {
			return Type.NUMBER;
		} else if (v instanceof String) {
			return Type.STRING;
		} else if (v instanceof Boolean) {
			return Type.BOOLEAN;
		} else {
			// TODO: Expand to cover arbitrary Java classes
			return Type.TOP;
		}
	}
	@Override
	public void addIndices(Set<Integer> indices, int depth) {
		return;
	}

	@Override
	public orc.ast.xml.expression.argument.Argument marshal() throws CompilationException {
		return new orc.ast.xml.expression.argument.Constant(v);
	}
}
