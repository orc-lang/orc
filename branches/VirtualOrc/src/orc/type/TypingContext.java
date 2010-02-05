//
// TypingContext.java -- Java class TypingContext
// Project OrcJava
//
// $Id$
//
// Created by dkitchin on Aug 29, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.type;

import java.util.List;

import orc.Config;
import orc.env.Env;
import orc.env.LookupFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.TypeException;

/**
 * 
 * The context used by the typechecker. Subsumes the variable typing context
 * and the type binding stack, each of which is accessed using a different method.
 * Also internalizes kinding checks, providing separate lookupType and lookupTycon
 * methods on the type binding stack.
 *
 * @author dkitchin
 */
public class TypingContext {

	protected Env<Type> varContext;
	protected Env<Type> typeContext;
	protected Config config;

	public TypingContext() {
		varContext = new Env<Type>();
		typeContext = new Env<Type>();
	}

	public TypingContext(final Config config) {
		varContext = new Env<Type>();
		typeContext = new Env<Type>();
		this.config = config;
	}

	public TypingContext(final Env<Type> varContext, final Env<Type> typeContext, final Config config) {
		this.varContext = varContext;
		this.typeContext = typeContext;
		this.config = config;
	}

	/**
	 * Find the binding for this program variable.
	 * 
	 * @param var
	 * @return The type of var in this context
	 */
	public Type lookupVar(final int var) {
		try {
			return varContext.lookup(var);
		} catch (final LookupFailureException e) {
			throw new OrcError(e);
		}
	}

	/**
	 * Find the binding for this type variable.
	 * 
	 * @param var
	 * @return The type of var in this context
	 */
	public Type lookupType(final int var) {
		try {
			return typeContext.lookup(var);
		} catch (final LookupFailureException e) {
			throw new OrcError(e);
		}
	}

	public TypingContext bindVar(final Type T) {
		return new TypingContext(varContext.extend(T), typeContext, config);
	}

	public TypingContext bindType(final Type T) {
		return new TypingContext(varContext, typeContext.extend(T), config);
	}

	public Type resolveSiteType(final String classname) throws TypeException {
		Class<?> cls;

		try {
			cls = config.loadClass(classname);
		} catch (final ClassNotFoundException e) {
			throw new TypeException("Failed to load class " + classname + " as an Orc external type. Class not found.");
		}

		if (!orc.type.Type.class.isAssignableFrom(cls)) {
			throw new TypeException("Class " + cls + " cannot be used as an Orc external type because it is not a subtype of orc.type.Type.");
		}

		try {
			return (orc.type.Type) cls.newInstance();
		} catch (final InstantiationException e) {
			throw new TypeException("Failed to load class " + cls + " as an external type. Instantiation error.", e);
		} catch (final IllegalAccessException e) {
			throw new TypeException("Failed to load class " + cls + " as an external type. Constructor is not accessible.");
		}

	}

	public Type resolveClassType(final String classname) throws TypeException {
		Class<?> cls;

		try {
			cls = config.loadClass(classname);
		} catch (final ClassNotFoundException e) {
			throw new TypeException("Failed to load class " + classname + " as an Orc class type. Class not found.");
		}

		return orc.type.Type.fromJavaClass(cls);
	}

	/*
	 * Entry point for substitution, so that we don't need to reveal
	 * the typing context with a getter.
	 */
	public Type subst(final Type T) throws TypeException {
		return T.subst(typeContext);
	}

	/*
	 * Convert a syntactic type to a real type, and then bind all
	 * of its free variables using the current type context.
	 */
	public orc.type.Type promote(final orc.ast.oil.type.Type t) throws TypeException {
		return t.transform(this).subst(typeContext);
	}

	public List<orc.type.Type> promoteAll(final List<orc.ast.oil.type.Type> ts) throws TypeException {
		return orc.type.Type.substAll(orc.ast.oil.type.Type.transformAll(ts, this), typeContext);
	}

}
