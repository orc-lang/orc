//
// TypeVariable.java -- Java class TypeVariable
// Project OrcJava
//
// $Id: TypeVariable.java 1553 2010-02-26 17:02:05Z jthywissen $
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.sites.compatibility.type;

/**
 * A bound type variable.
 * 
 * Subtype comparisons may occur between types with bound variables (such as between
 * the type bodies of parameterized arrow types), so there is a subtype relation
 * specified for type variables: it is simply variable equality.
 * 
 * @author dkitchin
 */
public class TypeVariable extends Type {

	public int index;
	public String name = null; // Optional program text name, used only for display purposes

	public TypeVariable(@SuppressWarnings("hiding") final int index) {
		this.index = index;
	}

	public TypeVariable(@SuppressWarnings("hiding") final int index, @SuppressWarnings("hiding") final String name) {
		this.index = index;
		this.name = name;
	}

	@Override
	public String toString() {
		return name == null ? "#" + index : name;
	}
}
