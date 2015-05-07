//
// Def.java -- Java class Def
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
import orc.error.SourceLocation;
import orc.error.compiletime.CompilationException;

/**
 * A unit of syntax that encapsulates an expression definition. 
 * 
 * Groups of mutually recursive definitions are scoped in the simplified abstract syntax tree by DeclareDefs.
 * 
 * @author dkitchin
 */
public class Def {

	public Variable name;
	public List<Variable> formals;
	public Expression body;
	protected List<TypeVariable> typeParams; /* Never null; if there are no type params, this will be an empty list */
	protected List<Type> argTypes; /* May be null, but only for defs derived from lambda, and only in a checking context */
	protected Type resultType; /* May be null to request inference */
	protected SourceLocation location;

	/**
	 * Note that the constructor takes a bound Var as a name parameter. This is because the
	 * binding of expression names occurs at the level of mutually recursive groups, not at
	 * the level of the individual definitions.
	 * @param location 
	 */
	public Def(final Variable name, final List<Variable> formals, final Expression body, final List<TypeVariable> typeParams, final List<Type> argTypes, final Type resultType, final SourceLocation location) {
		this.name = name;
		this.formals = formals;
		this.body = body;
		this.typeParams = typeParams != null ? typeParams : new LinkedList<TypeVariable>();
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.location = location;
	}

	public Def subst(final Argument a, final FreeVariable x) {
		return new Def(name, formals, body.subst(a, x), typeParams, argTypes, resultType, location);
	}

	public Def subMap(final Map<FreeVariable, ? extends Argument> m) {
		return new Def(name, formals, body.subMap(m), typeParams, argTypes, resultType, location);
	}

	public Def subst(final Type T, final FreeTypeVariable X) {

		return new Def(name, formals, body.subst(T, X), typeParams, Type.substAll(argTypes, T, X), Type.substMaybe(resultType, T, X), location);
	}

	public static List<Def> substAll(final List<Def> defs, final Argument a, final FreeVariable x) {
		final List<Def> newdefs = new LinkedList<Def>();
		for (final Def d : defs) {
			newdefs.add(d.subst(a, x));
		}
		return newdefs;
	}

	public static List<Def> substAll(final List<Def> defs, final Type T, final FreeTypeVariable X) {
		final List<Def> newdefs = new LinkedList<Def>();
		for (final Def d : defs) {
			newdefs.add(d.subst(T, X));
		}
		return newdefs;
	}

	// Does not validly compute the set of free vars if this definition is in a mutually recursive group.
	// That determination must be accounted for elsewhere.
	public Set<Variable> vars() {
		final Set<Variable> freeset = body.vars();
		freeset.remove(name);
		freeset.removeAll(formals);
		return freeset;
	}

	public orc.ast.oil.expression.Def convert(final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {

		final Env<Variable> newvars = vars.extendAll(formals);
		final Env<TypeVariable> newtypevars = typevars.extendAll(typeParams);

		final orc.ast.oil.type.Type newResultType = resultType != null ? resultType.convert(newtypevars) : null;

		return new orc.ast.oil.expression.Def(formals.size(), body.convert(newvars, newtypevars), typeParams.size(), Type.convertAll(argTypes, newtypevars), newResultType, location, name.name);

	}

	/**
	 * Convenience method, to apply convert to a list of defs.
	 * 
	 * @param ds A list of defs
	 * @param vars
	 * @param typevars
	 * @return The list of expressions, converted
	 * @throws CompilationException
	 */
	public static List<orc.ast.oil.expression.Def> convertAll(final List<Def> ds, final Env<Variable> vars, final Env<TypeVariable> typevars) throws CompilationException {

		if (ds != null) {
			final List<orc.ast.oil.expression.Def> newds = new LinkedList<orc.ast.oil.expression.Def>();

			for (final Def d : ds) {
				newds.add(d.convert(vars, typevars));
			}

			return newds;
		} else {
			return null;
		}
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append("def ");
		s.append(name);
		s.append('[');
		for (int i = 0; i < typeParams.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(typeParams.get(i));
		}
		s.append(']');
		s.append('(');
		if (argTypes != null) {
			for (int i = 0; i < argTypes.size(); i++) {
				if (i > 0) {
					s.append(", ");
				}
				s.append(argTypes.get(i));
			}
		}
		s.append(')');
		s.append(" :: ");
		s.append(resultType);
		s.append('\n');

		s.append("def ");
		s.append(name);
		s.append('(');
		for (int i = 0; i < formals.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(formals.get(i));
		}
		s.append(')');
		s.append(" = ");
		s.append(body);

		s.append('\n');

		return s.toString();
	}

}
