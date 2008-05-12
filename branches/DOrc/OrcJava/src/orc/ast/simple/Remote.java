/**
 * 
 */
package orc.ast.simple;

import java.util.Set;

import orc.ast.simple.arg.Argument;
import orc.ast.simple.arg.NamedVar;
import orc.ast.simple.arg.Var;
import orc.runtime.nodes.Node;

/**
 * An expression executed at a remote server (given by the second argument).
 * @author quark
 *
 */
public class Remote extends Expression {
	private Expression expression;
	private Argument server;
	public Remote(Argument server, Expression expression) {
		this.expression = expression;
		this.server = server;
	}

	@Override
	public Node compile(Node output) {
		return new orc.runtime.nodes.RemoteCall(server,
			expression.compile(new orc.runtime.nodes.RemoteReturn()),
			output);
	}

	@Override
	public Expression subst(Argument a, NamedVar x) {
		return new Remote(server.subst(a, x), expression.subst(a,x));
	}

	@Override
	public Set<Var> vars() {		
		Set<Var> s = expression.vars();
		server.addFree(s);
		return s;
	}
}