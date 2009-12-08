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

package orc.ast.extended.type;

import orc.ast.extended.security.SecurityLabel;

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
	 * @see orc.ast.extended.type.Type#simplify()
	 */
	@Override
	public orc.ast.simple.type.Type simplify() {
		return new orc.ast.simple.type.SecurityLabeledType(type.simplify(), label.simplify());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return type.toString() + label.toString();
	}

}
