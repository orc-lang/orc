package orc.ast.oil.expression.argument;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.Def;
import orc.ast.oil.expression.Expression;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.runtime.Token;
import orc.runtime.values.Value;

public abstract class Argument extends Expression {
	public abstract Object resolve(Env<Object> env);
	public abstract orc.ast.xml.expression.argument.Argument marshal() throws CompilationException;
	
	/* Reduce an argument list to a field name if that arg list
	 * is a singleton list of a field.
	 * Otherwise, return null.
	 */
	public static String asField(List<Argument> args) {
		if (args.size() == 1 && args.get(0) instanceof Field) {
			Field f = (Field)args.get(0);
			return f.key;
		}
		else {
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#populateContinuations()
	 */
	@Override
	public void populateContinuations() {
		// No children
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.oil.expression.Expression#enter(orc.runtime.Token)
	 */
	@Override
	public void enter(Token t) {
		Object v = Value.forceArg(t.lookup(this), t);
		
		if (v != Value.futureNotReady) {
			leave(t.setResult(v));
		}
	}
}