//
// Record.java -- Java class Record
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

package orc.lib.state;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import orc.ast.oil.expression.argument.Argument;
import orc.ast.oil.expression.argument.Constant;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.structured.DotType;

/**
 * @author quark
 */
public class Record extends EvalSite {
	private static class RecordInstance extends EvalSite {
		private final HashMap<String, Object> map = new HashMap<String, Object>();

		@Override
		public Object evaluate(final Args args) throws TokenException {
			final String field = args.fieldName();
			return map.get(field);
		}

		@Override
		public String toString() {
			return map.toString();
		}

		private void put(final String key, final Object value) {
			map.put(key, value);
		}
	}

	@Override
	public Object evaluate(final Args args) throws TokenException {
		final RecordInstance out = new RecordInstance();
		final Iterator<Object> argsi = args.iterator();
		while (argsi.hasNext()) {
			final Object keyo = argsi.next();
			String key;
			try {
				key = (String) keyo;
			} catch (final ClassCastException e) {
				throw new ArgumentTypeMismatchException(e);
			}
			if (!argsi.hasNext()) {
				throw new ArityMismatchException("Record key missing a value");
			}
			out.put(key, argsi.next());
		}
		return out;
	}

	private static class RecordBuilderType extends Type {

		/* Override the default type call implementation,
		 * using the string values of the args directly
		 * within the constructed dot type.
		 */
		@Override
		public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {

			int i = 0;
			final DotType dt = new DotType();
			String key = "";
			try {
				while (i < args.size()) {
					final Constant c = (Constant) args.get(i);
					key = (String) c.v;
					final Type t = args.get(i + 1).typesynth(ctx);
					dt.addField(key, t);
					i += 2;
				}
			} catch (final ClassCastException e) {
				throw new TypeException("Record field name at index " + i + " must be a string constant");
			} catch (final IndexOutOfBoundsException e) {
				throw new TypeException("Arity mismatch: no initial value given after field name '" + key + "'");
			}

			return dt;
		}

	}

	private static Type thisType = new RecordBuilderType();

	@Override
	public Type type() {
		return thisType;
	}
}
