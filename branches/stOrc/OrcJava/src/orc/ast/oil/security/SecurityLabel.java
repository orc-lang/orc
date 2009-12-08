//
// SecurityLabel.java -- Java class SecurityLabel
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

package orc.ast.oil.security;

/**
 * 
 *
 * @author jthywiss
 */
public class SecurityLabel {

	public final int level;

	public SecurityLabel(final int level) {
		this.level = level;
	}

	/** 
	 * Convert this OIL AST type into an actual label.
	 */
	public orc.security.labels.SecurityLabel transform(/*context?*/) {
		return new orc.security.labels.SecurityLabel(level);
	}

	@Override
	public String toString() {
		return "{" + Integer.toString(this.level) + "}";
	}

	//FIXME: Do we need any marshal / unmarshal methods or XML annotations here?
}
