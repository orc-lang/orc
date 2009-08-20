package orc.ast.oil.expression.argument;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import orc.ast.oil.Def;
import orc.ast.oil.expression.Expr;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public abstract class Arg extends Expr {
	public abstract Object resolve(Env<Object> env);
	public abstract orc.ast.oil.xml.Argument marshal() throws CompilationException;
	
	/* Reduce an argument list to a field name if that arg list
	 * is a singleton list of a field.
	 * Otherwise, return null.
	 */
	public static String asField(List<Arg> args) {
		if (args.size() == 1 && args.get(0) instanceof Field) {
			Field f = (Field)args.get(0);
			return f.key;
		}
		else {
			return null;
		}
	}
}