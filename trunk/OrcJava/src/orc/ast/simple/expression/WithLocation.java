package orc.ast.simple.expression;

import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVariable;
import orc.ast.simple.argument.Variable;
import orc.env.Env;
import orc.error.Located;
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * Annotate an expression with a source location.
 * @author quark
 */
public class WithLocation extends Expression implements Located {
	private final Expression expr;
	private final SourceLocation location;
	
	public WithLocation(final Expression expr, final SourceLocation location) {
		assert(location != null);
		this.expr = expr;
		this.location = location;
	}

	@Override
	public orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<String> typevars) throws CompilationException {
		return new orc.ast.oil.expression.WithLocation(expr.convert(vars, typevars), location);
	}

	@Override
	public Expression subst(Argument a, NamedVariable x) {
		return new WithLocation(expr.subst(a, x), location);
	}

	@Override
	public Set<Variable> vars() {
		return expr.vars();
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
}
