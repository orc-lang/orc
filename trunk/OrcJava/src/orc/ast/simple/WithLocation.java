package orc.ast.simple;

import java.util.Set;

import orc.ast.oil.expression.Expr;
import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.NamedVar;
import orc.ast.simple.argument.Var;
import orc.ast.simple.expression.Expression;
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
	public Expr convert(Env<Var> vars, Env<String> typevars) throws CompilationException {
		return new orc.ast.oil.WithLocation(expr.convert(vars, typevars), location);
	}

	@Override
	public Expression subst(Argument a, NamedVar x) {
		return new WithLocation(expr.subst(a, x), location);
	}

	@Override
	public Set<Var> vars() {
		return expr.vars();
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
}
