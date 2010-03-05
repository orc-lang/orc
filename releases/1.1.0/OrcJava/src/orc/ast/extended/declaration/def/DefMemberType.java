//
// DefMemberType.java -- Java class DefMemberType
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

package orc.ast.extended.declaration.def;

import java.util.List;

import orc.ast.extended.expression.Expression;
import orc.ast.extended.type.LambdaType;
import orc.ast.extended.type.Type;
import orc.ast.extended.visitor.Visitor;
import orc.error.compiletime.CompilationException;

/**
 * 
 * A declaration of the form:
 *
 * def f[T,...,T](T,...,T)(T,...,T)... :: T
 * 
 * This declares the signature of the function f. Type parameters [T,...,T]
 * may be an empty list. There may be one or more argument type groups (T,...,T).
 *
 * @author dkitchin
 */
public class DefMemberType extends DefMember {

	public List<List<Type>> argTypesList; // Must not be empty
	public Type resultType; // May be null
	public List<String> typeParams; // May be empty if there are no type parameters

	public DefMemberType(final String name, final List<List<Type>> argTypesList, final Type resultType, final List<String> typeParams) {
		this.name = name;
		this.argTypesList = argTypesList;
		this.resultType = resultType;
		this.typeParams = typeParams;
	}

	public String sigToString() {
		final StringBuilder s = new StringBuilder();

		s.append(name);

		if (typeParams != null) {
  		s.append('[');
  		s.append(Expression.join(typeParams, ", "));
  		s.append(']');
		}

		for (final List<Type> argTypes : argTypesList) {
			s.append('(');
			if (argTypes != null) {
			  s.append(Expression.join(argTypes, ","));
			}
			s.append(')');
		}

		s.append(" :: ");
		s.append(resultType);

		return s.toString();
	}

	@Override
	public String toString() {
		return "def " + sigToString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
	 */
	public <E> E accept(final Visitor<E> visitor) {
		return visitor.visit(this);
	}

	@Override
	public void extend(final AggregateDef adef) throws CompilationException {

		final LambdaType lt = new LambdaType(argTypesList, resultType, typeParams).uncurry();

		adef.setTypeParams(lt.typeParams);
		adef.setArgTypes(lt.argTypes.get(0));
		adef.setResultType(lt.resultType);
		adef.addLocation(getSourceLocation());
	}

}
