//
// SecurityLabel.java -- Java class SecurityLabel
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

package orc.ast.extended.security;


/**
 * Extended AST node representing a security label for a value
 *
 * @author jthywiss
 */
public class SecurityLabel {

	public static final SecurityLabel DEFAULT = new SecurityLabel("A0");

	public final String level;

	public SecurityLabel(final String level) {
		this.level = level;
	}

	/** 
	 * Convert this extended AST type into a simple AST type.
	 */
	public orc.ast.simple.security.SecurityLabel simplify() {
		return new orc.ast.simple.security.SecurityLabel(level);
	}

}
