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

package orc.security.labels;

import orc.error.compiletime.typing.TypeException;

/**
 * Security label for a value. This label can be composed of a confidentiality
 * part and an integrity part.  Labels form a lattice under a "restrictiveness"
 * relation.  
 *
 * @author jthywiss
 */
public class SecurityLabel {
	// When we get beyond experimenting, factor out interfaces for this
	// First experiment: a total order.  Lattices come later....

	public static final SecurityLabel DEFAULT = new SecurityLabel("A0");
	// NOTE: Keep the above in sync with package orc.ast.extended.security.SecurityLabel.DEFAULT
	
	/**
	 * The greatest / top label -- all labels are sublabels of this.
	 */
	public static final SecurityLabel TOPLABEL = new SecurityLabel("I9");

	/**
	 * The least / bottom label -- no labels are sublabels of this.
	 */
	public static final SecurityLabel BOTLABEL = new SecurityLabel("A0");

	public SecurityLabel(final String level) {
		this.level = level;
	}

	public boolean sublabel(final SecurityLabel that) {
		// equal -> true
		// less -> true
		// greater -> false
		// incomparable -> false
		
		// Quick & dirty for the two-character label lattice
		return (this.level.charAt(0) <= that.level.charAt(0) && this.level.charAt(1) <= that.level.charAt(1));
	}

	public void assertSublabel(final SecurityLabel that) throws TypeException {
		if (!this.sublabel(that)) {
			throw new TypeException("Expected label " + that + " or some sublabel, found label " + this + " instead.");
		}
	}

	public boolean superlabel(final SecurityLabel that) {
		return that.sublabel(this);
	}

	/* By default, equality is based on mutual sublabels.
	 */
	public boolean equal(final SecurityLabel that) {
		return this.sublabel(that) && that.sublabel(this);
	}

	private static char minChar(final char c1, final char c2) {
		if (c1 <= c2) {
			return c1;
		} else {
			return c2;
		}
	}

	private static char maxChar(final char c1, final char c2) {
		if (c1 >= c2) {
			return c1;
		} else {
			return c2;
		}
	}

	/* Find the join (least upper bound) in the sublabel lattice
	 * of this label and another label.
	 */
	public SecurityLabel join(final SecurityLabel that) {
		// Quick & dirty for the two-character label lattice
		return new SecurityLabel(Character.toString(maxChar(this.level.charAt(0), that.level.charAt(0))) + Character.toString(maxChar(this.level.charAt(1), that.level.charAt(1))));
	}

	/* Find the meet (greatest lower bound) in the sublabel lattice
	 * of this label and another label.
	 */
	public SecurityLabel meet(final SecurityLabel that) {
		return new SecurityLabel(Character.toString(minChar(this.level.charAt(0), that.level.charAt(0))) + Character.toString(minChar(this.level.charAt(1), that.level.charAt(1))));
	}

	@Override
	public String toString() {
		return "{" + level + "}";
	}

	private final String level;
}
