//
// DatatypeTycon.java -- Java class DatatypeTycon
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

package orc.type.tycon;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.Type;

/**
 * Type constructor encompassing all type-level information associated with
 * a datatype. This tycon will be passed as a type parameter to the Datatype
 * site, which when typechecked will return a tuple of the constructor types.
 * 
 * This may seem slightly obtuse, but it makes the earlier translation steps
 * easier while still guaranteeing that all type information can be discarded
 * from an OIL file without affecting its runtime semantics. If datatype
 * declarations simultaneously had static and dynamic semantics, that would
 * not be true. So, this is the isolated static component of the datatype
 * declaration.
 * 
 * @author dkitchin
 */
public class DatatypeTycon extends Tycon {

	String name;
	List<Variance> vs;
	List<List<Type>> cs;
	Object id; // A unique identifier used for equality

	public DatatypeTycon(final String name, final List<Variance> vs, final List<List<Type>> cs, final Object id) {
		this.name = name;
		this.vs = vs;
		this.cs = cs;
		this.id = id;
	}

	@Override
	public List<Variance> variances() {
		return vs;
	}

	public List<List<Type>> getConstructors() throws TypeException {

		/* Unfold this datatype once and return its list
		 * of constructors.
		 * 
		 * Note that each constructor will also have type
		 * parameters of the arity of this datatype.
		 */

		Env<Type> recctx = new Env<Type>().extend(this);

		// Protect the bound type variables of the constructors
		for (int i = 0; i < vs.size(); i++) {
			recctx = recctx.extend(null);
		}

		final List<List<Type>> newcs = new LinkedList<List<Type>>();
		for (final List<Type> c : cs) {
			newcs.add(Type.substAll(c, recctx));
		}

		return newcs;
	}

	@Override
	public Type subst(Env<Type> ctx) throws TypeException {

		/* Add context entries for the recursively bound
		 * datatype name itself, and for each of its
		 * type parameters.
		 */
		for (int i = 0; i < vs.size() + 1; i++) {
			ctx = ctx.extend(null);
		}

		final List<List<Type>> newcs = new LinkedList<List<Type>>();
		for (final List<Type> ts : cs) {
			newcs.add(Type.substAll(ts, ctx));
		}
		return new DatatypeTycon(name, vs, newcs, id);
	}

	@Override
	public Set<Integer> freeVars() {

		final Set<Integer> vars = new TreeSet<Integer>();
		for (final List<Type> ts : cs) {
			vars.addAll(Type.allFreeVars(ts));
		}

		return Type.shiftFreeVars(vars, vs.size() + 1);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(final Object that) {
		return that.getClass().equals(DatatypeTycon.class) && ((DatatypeTycon) that).id == id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}

	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		final orc.ast.xml.type.Type[][] newCs = new orc.ast.xml.type.Type[cs.size()][];
		int i = 0;
		for (final List<Type> c : cs) {
			newCs[i] = new orc.ast.xml.type.Type[c.size()];
			int j = 0;
			for (final Type t : c) {
				newCs[i][j] = t.marshal();
				++j;
			}
			++i;
		}
		return new orc.ast.xml.type.Datatype(name, newCs, vs.size());
	}
}
