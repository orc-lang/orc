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

package orc.values.sites.compatibility.type.structured;

import java.util.LinkedList;
import java.util.List;

import orc.values.sites.compatibility.type.Type;

@SuppressWarnings("hiding")
public class ArrowType extends Type {

	public List<Type> argTypes;
	public Type resultType;
	public int typeArity = 0;

	public ArrowType(final Type resultType) {
		this.argTypes = new LinkedList<Type>();
		this.resultType = resultType;
	}

	public ArrowType(final Type argType, final Type resultType) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(argType);
		this.resultType = resultType;
	}

	public ArrowType(final Type firstArgType, final Type secondArgType,  final Type resultType) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(firstArgType);
		argTypes.add(secondArgType);
		this.resultType = resultType;
	}

	public ArrowType( final List<Type> argTypes,  final Type resultType) {
		this.argTypes = argTypes;
		this.resultType = resultType;
	}

	public ArrowType( final Type resultType,  final int typeArity) {
		this.argTypes = new LinkedList<Type>();
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public ArrowType(final Type argType,  final Type resultType,  final int typeArity) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(argType);
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public ArrowType(final Type firstArgType, final Type secondArgType,  final Type resultType,  final int typeArity) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(firstArgType);
		argTypes.add(secondArgType);
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	public ArrowType( final List<Type> argTypes,  final Type resultType,  final int typeArity) {
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.typeArity = typeArity;
	}
}
