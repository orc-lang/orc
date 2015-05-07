//
// Tagged.java -- Java class Tagged
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class Tagged extends Value {
	@XmlAttribute
	public String tagName;
	@XmlElement(name = "element")
	public Object[] elements = new Object[] {};
	@XmlAttribute
	public int size;

	public Tagged() {
	}

	public Tagged(final String tagName, final Object[] elements) {
		this.tagName = tagName;
		this.size = elements.length;
		this.elements = elements;
	}

	@Override
	public String toString() {
		return super.toString() + "(" + tagName + ", " + Arrays.toString(elements) + ")";
	}
}
