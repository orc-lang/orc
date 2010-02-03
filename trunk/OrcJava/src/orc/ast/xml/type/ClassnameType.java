//
// ClassnameType.java -- Java class ClassnameType
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

package orc.ast.xml.type;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * A syntactic type which refers to a Java class (which we will treat as a type).
 * @author quark, dkitchin
 */
public class ClassnameType extends Type {
	@XmlAttribute(required = true)
	public String classname;

	public ClassnameType() {
	}

	public ClassnameType(final String classname) {
		this.classname = classname;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.ClassType(classname);
	}

	@Override
	public String toString() {
		return classname;
	}
}
