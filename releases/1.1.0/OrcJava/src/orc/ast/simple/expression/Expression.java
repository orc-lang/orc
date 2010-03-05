//
// Expression.java -- Java class Expression
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.simple.expression;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import orc.ast.simple.argument.Argument;
import orc.ast.simple.argument.FreeVariable;
import orc.ast.simple.argument.Variable;
import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.Type;
import orc.ast.simple.type.TypeVariable;
import orc.env.Env;
import orc.error.compiletime.CompilationException;

/**
 * Base class for the simplified abstract syntax tree.
 * 
 * @author dkitchin, wcook
 */
public abstract class Expression {
	/**
	 * Converts abstract syntax tree into a serializable form, used to generate
	 * portable .oil (Orc Intermediate Language) files.
	 * 
	 * @param vars	The vars environment, used in content addressable mode to 
	 * 				find the appropriate deBruijn index of a var.
	 * @param typevars The type vars environment, used in content addressable
	 * 				   mode to find the appropriate deBruijn index of a type var.
	 * @return A new node.
	 */
	public abstract orc.ast.oil.expression.Expression convert(Env<Variable> vars, Env<TypeVariable> typevars) throws CompilationException;

	/**
	 * Convenience method, to apply convert to a list of expressions.
	 * 
	 * @param es A list of expressions.
	 * @param vars
	 * @param typevars
	 * @return The list of expressions, after conversion
	 * @throws CompilationException
	 */
	public static List<orc.ast.oil.expression.Expression> convertAll(final List<Expression> es, final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {

		if (es != null) {
			final List<orc.ast.oil.expression.Expression> newes = new LinkedList<orc.ast.oil.expression.Expression>();

			for (final Expression e : es) {
				newes.add(e.convert(vars, typevars));
			}

			return newes;
		} else {
			return null;
		}
	}

	/**
	 * Performs the substitution [a/x], replacing occurrences of the free variable x
	 * with the new argument a (which could be any argument, including another variable).
	 * 
	 * @param a The replacing variable or value
	 * @param x The free variable whose occurrences will be replaced
	 * 
	 * @return A new copy of the expression with the substitution performed
	 */
	public abstract Expression subst(Argument a, FreeVariable x);

	/**
	 * Convenience method, to apply a substitution to a list of expressions.
	 */
	public static List<Expression> substAll(final List<Expression> es, final Argument a, final FreeVariable x) {
		final List<Expression> newes = new LinkedList<Expression>();
		for (final Expression e : es) {
			newes.add(e.subst(a, x));
		}
		return newes;
	}

	/**
	 * 
	 * Performs the substitution [v/x], replacing occurrences of the free variable x
	 * with the nameless variable v. Additionally, attach the name of x as documentation
	 * on the variable v so that it can be used later for debugging or other purposes.
	 * 
	 * This method delegates to the subst method after attaching the name. 
	 * 
	 * @param v The replacing variable
	 * @param x The free variable whose occurrences will be replaced
	 * 
	 * @return A new copy of the expression with the substitution performed
	 */
	public Expression subvar(final Variable v, final FreeVariable x) {
		v.name = x.name;
		return subst(v, x);
	}

	/**
	 * Perform a set of substitutions defined by a map.
	 * For each x |-> a in the map, the substitution [a/x] occurs.
	 * 
	 * If a is a nameless variable v, the name of the corresponding
	 * x will be attached to it.
	 * 
	 * @param m
	 */
	public Expression subMap(final Map<FreeVariable, ? extends Argument> m) {
		Expression result = this;

		for (final FreeVariable x : m.keySet()) {
			final Argument a = m.get(x);
			if (a instanceof Variable) {
				result = result.subvar((Variable) a, x);
			} else {
				result = result.subst(a, x);
			}
		}

		return result;
	}

	/**
	 * Performs the substitution [T/X], replacing occurrences of the free type variable X
	 * with the type T (which could be any type, including another variable).
	 * 
	 * @param T The replacing type
	 * @param X The free type variable whose occurrences will be replaced
	 * 
	 * @return A new copy of the expression with the substitution performed
	 */
	public abstract Expression subst(Type T, FreeTypeVariable X);

	/**
	 * 
	 * Performs the substitution [U/X], replacing occurrences of the free type variable X
	 * with the nameless type variable U. Additionally, attach the name of X as documentation
	 * on U so that it can be used later for debugging or other purposes.
	 * 
	 * This method delegates to the subst method after attaching the name. 
	 * 
	 * @param U The replacing type variable
	 * @param X The free type variable whose occurrences will be replaced
	 * 
	 * @return A new copy of the type with the substitution performed
	 */
	public Expression subvar(final TypeVariable U, final FreeTypeVariable X) {
		U.name = X.name;
		return this.subst(U, X);
	}

	/**
	 * Find the set of all Variables (note: not FreeVariables) that are not bound within this expression.
	 */
	public abstract Set<Variable> vars();
}
