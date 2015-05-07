//
// LambdaType.java -- Java class LambdaType
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

package orc.ast.extended.type;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import orc.ast.simple.type.FreeTypeVariable;
import orc.ast.simple.type.TypeVariable;

/**
 * A lambda type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author dkitchin
 */
public class LambdaType extends Type {

	public List<String> typeParams;
	public List<List<Type>> argTypes;
	public Type resultType;

	public LambdaType(final List<List<Type>> argTypes, final Type resultType, final List<String> typeParams) {
		this.typeParams = typeParams;
		this.argTypes = argTypes;
		this.resultType = resultType;
	}

	@Override
	public orc.ast.simple.type.Type simplify() {

		final LambdaType newLambda = this.uncurry();

		final Map<FreeTypeVariable, orc.ast.simple.type.Type> bindings = new TreeMap<FreeTypeVariable, orc.ast.simple.type.Type>();

		final List<TypeVariable> newTypeParams = new LinkedList<TypeVariable>();
		if (newLambda.typeParams != null) {
			for (final String s : newLambda.typeParams) {
				final TypeVariable Y = new TypeVariable();
				final FreeTypeVariable X = new FreeTypeVariable(s);
				newTypeParams.add(Y);
				bindings.put(X, Y);
			}
		}

		List<orc.ast.simple.type.Type> newArgTypes = null;
		final List<Type> lamArgTypes = newLambda.argTypes.get(0);
		if (lamArgTypes != null) {
			newArgTypes = new LinkedList<orc.ast.simple.type.Type>();
			for (final Type t : lamArgTypes) {
				newArgTypes.add(t.simplify().subMap(bindings));
			}
		}

		orc.ast.simple.type.Type newResultType;
		if (newLambda.resultType == null) {
			newResultType = null;
		} else {
			newResultType = newLambda.resultType.simplify().subMap(bindings);
		}

		return new orc.ast.simple.type.ArrowType(newArgTypes, newResultType, newTypeParams);
	}

	/**
	 * From a lambda type with multiple type argument groups:
	 * 
	 * lambda[X](T)(T)(T) :: T
	 * 
	 * create a chain of lambdas each with a single such group:
	 * 
	 * lambda[X](T) :: (lambda(T) :: (lambda(T) :: T))
	 * 
	 * If there is only one type group, just return an identical copy.
	 * 
	 */
	public LambdaType uncurry() {

		final ListIterator<List<Type>> it = argTypes.listIterator(argTypes.size());

		List<List<Type>> singleton = new LinkedList<List<Type>>();
		singleton.add(it.previous());
		LambdaType result = new LambdaType(singleton, resultType, new LinkedList<String>());

		while (it.hasPrevious()) {
			singleton = new LinkedList<List<Type>>();
			singleton.add(it.previous());
			result = new LambdaType(singleton, result, new LinkedList<String>());
		}

		result.typeParams = typeParams;
		return result;
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append('(');

		s.append("lambda ");
		if (typeParams != null) {
			s.append('[');
			for (int i = 0; i < typeParams.size(); i++) {
				if (i > 0) {
					s.append(", ");
				}
				s.append(typeParams.get(i));
			}
			s.append(']');
		}
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

		s.append(')');

		return s.toString();
	}

}
