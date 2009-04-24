package orc.ast.oil.arg;

import orc.ast.oil.Expr;
import orc.env.Env;

public abstract class Arg extends Expr {
	public abstract Object resolve(Env<Object> env);
}