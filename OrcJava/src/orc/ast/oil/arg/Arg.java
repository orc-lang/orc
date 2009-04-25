package orc.ast.oil.arg;

import orc.ast.oil.Expr;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

public abstract class Arg extends Expr {
	public abstract Object resolve(Env<Object> env);
	public abstract orc.ast.oil.xml.Argument marshal() throws CompilationException;
}