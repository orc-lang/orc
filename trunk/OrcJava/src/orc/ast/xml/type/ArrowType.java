//
// ArrowType.java -- Java class ArrowType
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 * A syntactic arrow type: lambda[X,...,X](T,...,T) :: T
 * 
 * @author quark, dkitchin
 */
public class ArrowType extends Type {
	@XmlElementWrapper(required = true)
	@XmlElement(name = "argType")
	public Type[] argTypes;
	@XmlElement(required = true)
	public Type resultType;
	@XmlAttribute(required = true)
	public int typeArity;

	public ArrowType() {
	}

	public ArrowType(final Type[] argTypes, final Type resultType, final int typeArity) {
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	@Override
	public orc.ast.oil.type.Type unmarshal() {
		return new orc.ast.oil.type.ArrowType(Type.unmarshalAll(argTypes), resultType.unmarshal(), typeArity);
	}
}
