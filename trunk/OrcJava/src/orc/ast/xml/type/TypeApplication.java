//
// TypeApplication.java -- Java class TypeApplication
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

import javax.xml.bind.annotation.XmlElement;

/**
 * A type instantiation with explicit type parameters: T[T,..,T]
 * 
 * @author dkitchin
 */
public class TypeApplication extends Type {
	@XmlElement(required = true)
	public Type constructor;
	@XmlElement(name = "param", required = true)
	public Type[] params;

	public TypeApplication() {
	}

	public TypeApplication(final Type ty, final Type[] params) {
		this.constructor = ty;
		this.params = params;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.TypeApplication(constructor.unmarshal(), Type.unmarshalAll(params));
	}
}
