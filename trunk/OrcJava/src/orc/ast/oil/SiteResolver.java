package orc.ast.oil;

import orc.Config;
import orc.ast.oil.expression.Expr;
import orc.ast.oil.expression.argument.Constant;
import orc.ast.oil.expression.argument.ResolvedSite;
import orc.ast.oil.expression.argument.Site;
import orc.error.OrcException;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.SiteResolutionException;
import orc.error.compiletime.typing.MissingTypeException;
import orc.type.Type;

/**
 * Resolve sites in an expression. Must be done before typechecking,
 * compilation to DAG, or other analysis that uses site metadata.
 * @author quark
 */
public class SiteResolver extends Transformer {
	private Config config;
	
	/** Hack to allow the visitor to throw an exception. */
	private static class SiteResolverException extends RuntimeException {
		public SiteResolverException(CompilationException e) {
			super(e);
		}
		public CompilationException getCause() {
			return (CompilationException)super.getCause();
		}
	}
	
	/** Do not call this directly. */
	private SiteResolver(Config config) {
		this.config = config;
	}
	
	/** Call this to run the resolver on an expression. */
	public static Expr resolve(Expr expr, Config config) throws CompilationException {
		try {
			return expr.accept(new SiteResolver(config));
		} catch (SiteResolverException e) {
			throw e.getCause();
		}
	}

	@Override
	public Expr visit(Site arg) {
		try {
			return arg.resolveSites(config);
		} catch (SiteResolutionException e) {
			throw new SiteResolverException(e);
		}
	}

	@Override
	public Type visit(Type type) {
		try {
			return type.resolveSites(config);
		} catch (MissingTypeException e) {
			throw new SiteResolverException(e);
		}
	}
}
