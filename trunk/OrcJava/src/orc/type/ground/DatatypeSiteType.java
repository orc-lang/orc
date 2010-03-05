//
// DatatypeSiteType.java -- Java class DatatypeSiteType
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

package orc.type.ground;

import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.TypingContext;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;

public class DatatypeSiteType extends Type {

	@Override
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {

		assert typeActuals.size() == 1;
		final orc.type.tycon.DatatypeTycon dt = (orc.type.tycon.DatatypeTycon) typeActuals.get(0);

		/* Make sure each argument is a string */
		for (final Argument a : args) {
			a.typecheck(ctx, Type.STRING);
		}

		/* Find the type arity for each constructor */
		final int cArity = dt.variances().size();

		/* Manufacture the result type as an instance of
		 * the datatype. If it has parameters, apply
		 * it to the bound parameters.
		 */
		Type cResult = dt;
		if (cArity > 0) {
			final List<Type> params = new LinkedList<Type>();
			for (int i = cArity - 1; i >= 0; i--) {
				params.add(new TypeVariable(i));
			}
			cResult = dt.instance(params);
		}

		final List<Type> cTypes = new LinkedList<Type>();
		for (final List<Type> cArgs : dt.getConstructors()) {
			final Type construct = new ArrowType(cArgs, cResult, cArity);
			final Type destruct = new ArrowType(cResult, LetType.condense(cArgs), cArity);

			final DotType both = new DotType(construct);
			both.addField("?", destruct);
			cTypes.add(both);
		}

		return LetType.condense(cTypes);
	}

}
