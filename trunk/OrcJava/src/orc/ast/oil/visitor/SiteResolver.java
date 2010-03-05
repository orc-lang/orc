//
// SiteResolver.java -- Java class SiteResolver
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.visitor;

import orc.Config;
import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.argument.Site;
import orc.ast.oil.type.Type;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.SiteResolutionException;

/**
 * Resolve sites in an expression. Must be done before typechecking,
 * compilation to DAG, or other analysis that uses site metadata.
 * @author quark
 */
public class SiteResolver extends Transformer {
	private final Config config;

	/** Hack to allow the visitor to throw an exception. */
	private static class SiteResolverException extends RuntimeException {
		public SiteResolverException(final CompilationException e) {
			super(e);
		}

		@Override
		public CompilationException getCause() {
			return (CompilationException) super.getCause();
		}
	}

	/** Do not call this directly. */
	private SiteResolver(final Config config) {
		this.config = config;
	}

	/** Call this to run the resolver on an expression. */
	public static Expression resolve(final Expression expr, final Config config) throws CompilationException {
		try {
			return expr.accept(new SiteResolver(config));
		} catch (final SiteResolverException e) {
			throw e.getCause();
		}
	}

	@Override
	public Expression visit(final Site arg) {
		try {
			return arg.resolveSites(config);
		} catch (final SiteResolutionException e) {
			throw new SiteResolverException(e);
		}
	}

	@Override
	public Type visit(final Type type) {
		// TODO: fix this, if it even needs to be fixed
		//		try {
		//			return type.resolveSites(config);
		//		} catch (MissingTypeException e) {
		//			throw new SiteResolverException(e);
		//		}
		return type;
	}
}
