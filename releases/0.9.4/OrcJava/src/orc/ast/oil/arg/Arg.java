package orc.ast.oil.arg;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Set;

import orc.ast.oil.Def;
import orc.ast.oil.Expr;
import orc.env.Env;
import orc.runtime.values.Future;
import orc.runtime.values.Value;

public abstract class Arg extends Expr {
	
	public orc.runtime.nodes.Node compile(orc.runtime.nodes.Node output) {
		return new orc.runtime.nodes.Let(this, output);
	}
	
	public abstract <T> T resolve(Env<T> env);
}