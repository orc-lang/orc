//
// List.java -- Java class List
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

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;

public class List extends Value {
	@XmlElement(name = "element")
	public Object[] elements = new Object[] {};

	public List() {
	}

	public List(final Object[] elements) {
		this.elements = elements;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + Arrays.toString(elements) + ")";
	}
}
