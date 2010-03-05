//
// SiteType.java -- Java class SiteType
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
 * A syntactic type corresponding to a Java class implementing a type.
 * 
 * In order to convert this to an actual type, the Java class must be
 * a subtype of orc.type.Type
 * 
 * @author dkitchin
 *
 */
public class SiteType extends Type {
	@XmlAttribute(required = true)
	public String classname;

	public SiteType() {
	}

	public SiteType(final String classname) {
		this.classname = classname;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.SiteType(classname);
	}
}
