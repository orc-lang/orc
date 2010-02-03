//
// StringType.java -- Java class StringType
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

import java.util.List;

import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.java.ClassTycon;

public class StringType extends Type {

	@Override
	public String toString() {
		return "String";
	}

	@Override
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {
		return new ClassTycon(java.lang.String.class).instance().call(ctx, args, typeActuals);
	}

	@Override
	public Class javaCounterpart() {
		return java.lang.String.class;
	}
}
