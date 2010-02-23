//
// Call.java -- Java class Call
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

package orc.ast.xml.expression;

import java.util.Arrays;
import java.util.LinkedList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import orc.Config;
import orc.ast.xml.expression.argument.Argument;
import orc.ast.xml.type.Type;
import orc.error.compiletime.CompilationException;

public class Call extends Expression {
	@XmlElement(required = true)
	public Argument callee;
	@XmlElementWrapper(required = true)
	@XmlElement(name = "argument")
	public Argument[] arguments;

	@XmlElementWrapper(required = false)
	@XmlElement(name = "typeArgs")
	public Type[] typeArgs;

	public Call() {
	}

	public Call(final Argument callee, final Argument[] arguments, final Type[] typeArgs) {
		this.callee = callee;
		this.arguments = arguments;
		this.typeArgs = typeArgs;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + callee + ", " + Arrays.toString(arguments) + ")";
	}

	@Override
	public orc.ast.oil.expression.Expression unmarshal(final Config config) throws CompilationException {
		final LinkedList<orc.ast.oil.expression.argument.Argument> args = new LinkedList<orc.ast.oil.expression.argument.Argument>();
		for (final Argument a : arguments) {
			args.add(a.unmarshal(config));
		}
		final orc.ast.oil.expression.Expression out = new orc.ast.oil.expression.Call(callee.unmarshal(config), args, Type.unmarshalAll(typeArgs));
		return out;
	}
}
