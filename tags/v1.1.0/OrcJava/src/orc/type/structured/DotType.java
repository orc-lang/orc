//
// DotType.java -- Java class DotType
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

package orc.type.structured;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Field;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;

/**
 * Composite type for sites which can receive messages (using the . notation)
 * 
 * A DotType is created with an optional default type (to be used when the
 * site is called with something other than a message), and then type entries
 * for each understood message are added using addField.
 * 
 * @author dkitchin
 */
public class DotType extends Type {

	public static final Type NODEFAULT = new NoDefaultType();
	Type defaultType;
	Map<String, Type> fieldMap;

	public DotType() {
		this.defaultType = NODEFAULT;
		fieldMap = new TreeMap<String, Type>();
	}

	public DotType(final Type defaultType) {
		this.defaultType = defaultType;
		fieldMap = new TreeMap<String, Type>();
	}

	public DotType addField(final String key, final Type T) {
		fieldMap.put(key, T);
		return this;
	}

	@Override
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {

		if (args.size() == 1 && args.get(0) instanceof Field) {
			final Field f = (Field) args.get(0);
			return fieldMap.get(f.key);
		} else {
			return defaultType.call(ctx, args, typeActuals);
		}
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {
		return defaultType.subtype(that) || super.subtype(that);
	}

	/* A call without explicit args passed is assumed to be a call to the default type */
	@Override
	public Type call(final List<Type> args) throws TypeException {
		return defaultType.call(args);
	}

	@Override
	public Set<Integer> freeVars() {

		final Set<Integer> vars = Type.allFreeVars(fieldMap.values());
		vars.addAll(defaultType.freeVars());

		return vars;
	}

	@Override
	public String toString() {

		final StringBuilder s = new StringBuilder();
		String sep = "";

		if (!(defaultType instanceof NoDefaultType)) {
			s.append('(');
			s.append(defaultType);
			s.append(" & ");
		}
		s.append('{');
		for (final String f : fieldMap.keySet()) {
			s.append(sep);
			sep = ", ";
			s.append(f + " :: ");
			s.append(fieldMap.get(f));
		}
		s.append('}');
		if (!(defaultType instanceof NoDefaultType)) {
			s.append(')');
		}

		return s.toString();
	}

}

final class NoDefaultType extends Type {

	@Override
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {
		throw new TypeException("This site has no default behavior; it can only be called via messages");
	}

	@Override
	public Type call(final List<Type> args) throws TypeException {
		throw new TypeException("This site has no default behavior; it can only be called via messages");
	}

	@Override
	public String toString() {
		return "no_default_type";
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {
		return false;
	}
}
