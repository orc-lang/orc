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

package orc.ast.simple.security;

/**
 * 
 *
 * @author jthywiss
 */
public class SecurityLabel {

	public final String level;

	public SecurityLabel(final String level) {
		this.level = level;
	}

	/** 
	 * Convert this simple AST type into a OIL AST type.
	 */
	public orc.ast.oil.security.SecurityLabel convert() {
		return new orc.ast.oil.security.SecurityLabel(level);
	}

}
