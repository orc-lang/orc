//
// UnrepresentableValue.java -- Java class UnrepresentableValue
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.values;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * FIXME: this is a hack so I can get everything to compile
 * without worrying about representations for weird values like
 * closures and sites. Long-term, this class should be removed.
 * 
 * @author quark
 */
public class UnrepresentableValue extends Value {
	@XmlAttribute
	private String description;

	public UnrepresentableValue() {
	}

	public UnrepresentableValue(final String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + description + ")";
	}
}
