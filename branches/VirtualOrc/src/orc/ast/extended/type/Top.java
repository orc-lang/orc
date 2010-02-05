//
// Top.java -- Java class Top
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

package orc.ast.extended.type;

/**
 * The type 'Top', supertype of all other types.
 * 
 * @author dkitchin
 *
 */
public class Top extends Type {

	@Override
	public orc.ast.simple.type.Type simplify() {
		return orc.ast.simple.type.Type.TOP;
	}

	@Override
	public String toString() {
		return "Top";
	}
}
