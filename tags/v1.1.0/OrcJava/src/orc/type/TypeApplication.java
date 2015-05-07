//
// TypeApplication.java -- Java class TypeApplication
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

package orc.type;

import java.util.List;
import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;

/**
 * An unevaluated type application.
 * 
 * @author dkitchin
 */
public class TypeApplication extends Type {

	public Type ty;
	public List<Type> params;

	public TypeApplication(final Type ty, final List<Type> params) {
		this.ty = ty;
		assert params.size() > 0;
		this.params = params;
	}

	@Override
	public Type subst(final Env<Type> ctx) throws TypeException {

		final Type newty = ty.subst(ctx);
		final List<Type> newparams = Type.substAll(params, ctx);

		/* If the constructor is now bound, create an instance */
		if (newty.freeVars().isEmpty()) {
			return newty.asTycon().instance(newparams);
		} else {
			return new TypeApplication(newty, newparams);
		}

	}

	@Override
	public Set<Integer> freeVars() {

		final Set<Integer> vars = Type.allFreeVars(params);
		vars.addAll(ty.freeVars());

		return vars;
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();

		s.append(ty);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(params.get(i));
		}
		s.append(']');

		return s.toString();
	}

	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		final orc.ast.xml.type.Type[] newParams = new orc.ast.xml.type.Type[params.size()];
		int i = 0;
		for (final Type t : params) {
			newParams[i] = t.marshal();
			++i;
		}
		return new orc.ast.xml.type.TypeApplication(ty.marshal(), newParams);
	}
}
