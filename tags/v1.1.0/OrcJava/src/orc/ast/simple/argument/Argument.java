//
// Argument.java -- Java class Argument
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

package orc.ast.simple.argument;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.env.Env;
import orc.error.Locatable;
import orc.error.SourceLocation;
import orc.error.compiletime.UnboundVariableException;

/**
 * An AST class (distinct from Expression) which contains arguments to calls.
 * These arguments may either be variable names or constant values.
 * 
 * Note that in the call M(x,y), M is also technically an argument of the call.
 * This allows variables to be used in call position.
 * 
 * @author dkitchin
 */
public abstract class Argument implements Serializable, Locatable {
	private SourceLocation location;

	public Argument subst(final Argument newArg, final FreeVariable oldArg) {
		return equals(oldArg) ? newArg : this;
	}

	/**
	 * Convenience method, to apply a substitution to a list of arguments.
	 */
	public static List<Argument> substAll(final List<Argument> args, final Argument a, final FreeVariable x) {
		final List<Argument> newargs = new LinkedList<Argument>();
		for (final Argument arg : args) {
			newargs.add(arg.subst(a, x));
		}
		return newargs;
	}

	public void addFree(final Set<Variable> freeset) {
		// Do nothing; overridden for arguments which may
		// be considered free in an expression
	}

	/**
	 * Convert to DeBruijn index.
	 */
	public abstract orc.ast.oil.expression.argument.Argument convert(Env<Variable> vars) throws UnboundVariableException;

	public static List<orc.ast.oil.expression.argument.Argument> convertAll(final List<Argument> as, final Env<Variable> vars) throws UnboundVariableException {

		final List<orc.ast.oil.expression.argument.Argument> newas = new LinkedList<orc.ast.oil.expression.argument.Argument>();

		for (final Argument a : as) {
			newas.add(a.convert(vars));
		}

		return newas;
	}

	public void setSourceLocation(final SourceLocation location) {
		this.location = location;
	}

	public SourceLocation getSourceLocation() {
		return location;
	}
}
