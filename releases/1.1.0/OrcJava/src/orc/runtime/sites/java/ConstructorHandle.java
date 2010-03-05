//
// ConstructorHandle.java -- Java class ConstructorHandle
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

package orc.runtime.sites.java;

import java.lang.reflect.Constructor;

public class ConstructorHandle extends InvokableHandle<Constructor> {
	public ConstructorHandle(final Constructor[] constructors) {
		super("<init>", constructors);
	}

	@Override
	protected Class[] getParameterTypes(final Constructor c) {
		return c.getParameterTypes();
	}

	@Override
	protected int getModifiers(final Constructor c) {
		return c.getModifiers();
	}
}
