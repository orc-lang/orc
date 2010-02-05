//
// ClassTycon.java -- Java class ClassTycon
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

package orc.type.java;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.lib.state.types.RefType;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

public class ClassTycon extends Tycon {

	public Class cls;

	/* This constructor is rarely used directly.
	 * Try Type.fromJavaClass instead.
	 */
	public ClassTycon(final Class cls) {
		this.cls = cls;
	}

	@Override
	public boolean subtype(final Type that) throws TypeException {

		// All tycons are subtypes of Top
		if (that.isTop()) {
			return true;
		}

		if (that instanceof ClassTycon) {
			final ClassTycon ct = (ClassTycon) that;

			// If this is not a generic class, just check Java subtyping.
			if (cls.getTypeParameters().length == 0) {
				return ct.cls.isAssignableFrom(cls);
			}

			// Otherwise, check for class equality.
			return ct.cls.equals(cls);
		}

		return false;
	}

	@Override
	public List<Variance> variances() {
		/* 
		 * All Java type parameters should be considered invariant, to be safe.
		 */
		final List<Variance> vs = new LinkedList<Variance>();
		for (int i = 0; i < cls.getTypeParameters().length; i++) {
			vs.add(Variance.INVARIANT);
		}
		return vs;
	}

	@Override
	public Type makeCallableInstance(final List<Type> params) throws TypeArityException {
		return new CallableJavaInstance(cls, Type.makeJavaCtx(cls, params));
	}

	@Override
	public String toString() {
		return cls.getName().toString();
	}

}

class CallableJavaInstance extends Type {

	Class cls;
	Map<java.lang.reflect.TypeVariable, Type> javaCtx;

	public CallableJavaInstance(final Class cls, final Map<java.lang.reflect.TypeVariable, Type> javaCtx) {
		this.cls = cls;
		this.javaCtx = javaCtx;
	}

	@Override
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {

		final String f = Argument.asField(args);

		if (f != null) {
			final List<Method> matchingMethods = new LinkedList<Method>();
			for (final Method m : cls.getMethods()) {
				if (m.getName().equals(f)) {
					matchingMethods.add(m);
				}
			}

			if (!matchingMethods.isEmpty()) {
				return Type.fromJavaMethods(matchingMethods, javaCtx);
			} else {
				// No method matches. Try fields.
				for (final java.lang.reflect.Field fld : cls.getFields()) {
					if (fld.getName().equals(f)) {
						return new RefType().instance(Type.fromJavaType(fld.getGenericType(), javaCtx));
					}
				}

				// Neither a method nor a field
				throw new TypeException("'" + f + "' is not a member of " + cls.getName());
			}
		} else {
			// Look for the 'apply' method

			final List<Method> matchingMethods = new LinkedList<Method>();
			for (final Method m : cls.getMethods()) {
				if (m.getName().equals("apply")) {
					matchingMethods.add(m);
				}
			}

			if (!matchingMethods.isEmpty()) {
				final Type target = Type.fromJavaMethods(matchingMethods, javaCtx);
				return target.call(ctx, args, typeActuals);
			} else {
				throw new TypeException("This Java class does not implement the 'apply' method, so it has no default site behavior. Use a method call.");
			}
		}

	}

}
