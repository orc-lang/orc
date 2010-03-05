//
// RenameVariables.java -- Java interface RenameVariables
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

import orc.ast.oil.expression.Expression;
import orc.ast.oil.expression.argument.Variable;

/**
 * Renumber variables in an expression according
 * to some arbitrary mapping (relative to the context
 * of the expression).
 * @author quark
 */
public class RenameVariables extends Walker {
	public interface Renamer {
		public int rename(int var);
	}

	public static void rename(final Expression expr, final Renamer r) {
		expr.accept(new RenameVariables(r));
	}

	private int depth = 0;
	private final Renamer renamer;

	private RenameVariables(final Renamer renamer) {
		this.renamer = renamer;
	}

	@Override
	public void enterScope(final int n) {
		depth += n;
	}

	@Override
	public void leaveScope(final int n) {
		depth -= n;
	}

	@Override
	public void leave(final Variable arg) {
		arg.index = depth + renamer.rename(arg.index - depth);
	}
}
