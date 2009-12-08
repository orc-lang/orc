//
// SecurityLabeledType.java -- Java class SecurityLabeledType
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on Dec 7, 2009.
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

import orc.Config;
import orc.ast.oil.expression.argument.Argument;
import orc.env.Env;
import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.security.labels.SecurityLabel;
import orc.type.inference.Constraint;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

/**
 * 
 *
 * @author jthywiss
 */
public class SecurityLabeledType extends Type {

	public final Type type;
	public final SecurityLabel label;

	/**
	 * Constructs an object of class SecurityLabeledType.
	 *
	 */
	public SecurityLabeledType(final Type type, final SecurityLabel label) {
		super();
		if (type == null || label == null) {
			throw new NullPointerException("SecurityLabeledType constructor received null parameter");
		}
		this.type = type;
		this.label = label;
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#subtype(orc.type.Type)
	 */
	@Override
	public boolean subtype(final Type that) throws TypeException {
		if (that instanceof SecurityLabeledType) {
			final SecurityLabeledType thatSlt = (SecurityLabeledType) that;
			return type.subtype(thatSlt.type) && label.sublabel(thatSlt.label);
		} else {
			return type.subtype(that) && label.sublabel(SecurityLabel.TOP);
		}
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#join(orc.type.Type)
	 */
	@Override
	public Type join(final Type that) throws TypeException {
		if (that instanceof SecurityLabeledType) {
			final SecurityLabeledType thatSlt = (SecurityLabeledType) that;
			return new SecurityLabeledType(type.join(thatSlt.type), label.join(thatSlt.label));
		} else {
			return new SecurityLabeledType(type.join(that), label.join(SecurityLabel.TOP));
		}
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#meet(orc.type.Type)
	 */
	@Override
	public Type meet(final Type that) throws TypeException {
		if (that instanceof SecurityLabeledType) {
			final SecurityLabeledType thatSlt = (SecurityLabeledType) that;
			return new SecurityLabeledType(type.meet(thatSlt.type), label.meet(thatSlt.label));
		} else {
			return new SecurityLabeledType(type.meet(that), label.meet(SecurityLabel.TOP));
		}
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#call(orc.type.TypingContext, java.util.List, java.util.List)
	 */
	@Override
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {
		//TODO: Check correct handling of label
		return type.call(ctx, args, typeActuals);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#call(java.util.List)
	 */
	@Override
	public Type call(final List<Type> args) throws TypeException {
		//TODO: Check correct handling of label
		return type.call(args);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#toString()
	 */
	@Override
	public String toString() {
		return type.toString() + label.toString();
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#subst(orc.env.Env)
	 */
	@Override
	public Type subst(final Env<Type> ctx) throws TypeException {
		return new SecurityLabeledType(type.subst(ctx), label);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#variances()
	 */
	@Override
	public List<Variance> variances() {
		//TODO: Check correct handling of label
		return type.variances();
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#unwrapAs(orc.type.Type)
	 */
	@Override
	public Type unwrapAs(final Type T) throws TypeException {
		//TODO: Check correct handling of label
		return type.unwrapAs(T);
		// or is it: return new SecurityLabeledType(type.unwrapAs(T), label);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#asTycon()
	 */
	@Override
	public Tycon asTycon() throws TypeException {
		//TODO: Check correct handling of label
		return type.asTycon();
		// or is it: return new SecurityLabeledType(type.asTycon(), label);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#freeVars()
	 */
	@Override
	public Set<Integer> freeVars() {
		return type.freeVars();
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#closed()
	 */
	@Override
	public boolean closed() {
		return type.closed();
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#findVariance(java.lang.Integer)
	 */
	@Override
	public Variance findVariance(final Integer var) {
		return type.findVariance(var);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#promote(orc.env.Env)
	 */
	@Override
	public Type promote(final Env<Boolean> V) throws TypeException {
		// TODO Auto-generated method stub
		return super.promote(V);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#demote(orc.env.Env)
	 */
	@Override
	public Type demote(final Env<Boolean> V) throws TypeException {
		// TODO Auto-generated method stub
		return super.demote(V);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#addConstraints(orc.env.Env, orc.type.Type, orc.type.inference.Constraint[])
	 */
	@Override
	public void addConstraints(final Env<Boolean> VX, final Type T, final Constraint[] C) throws TypeException {
		// TODO Auto-generated method stub
		super.addConstraints(VX, T, C);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#javaCounterpart()
	 */
	@Override
	public Class javaCounterpart() {
		return type.javaCounterpart();
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#resolveSites(orc.Config)
	 */
	@Override
	public Type resolveSites(final Config config) throws MissingTypeException {
		//TODO: Check correct handling of label
		return type.resolveSites(config);
	}

	/* (non-Javadoc)
	 * @see orc.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		// TODO Auto-generated method stub
		return super.marshal();
	}

}
