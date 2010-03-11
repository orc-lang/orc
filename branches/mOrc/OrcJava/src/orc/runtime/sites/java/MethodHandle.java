//
// MethodHandle.java -- Java class MethodHandle
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

import java.lang.reflect.Method;

/**
 * Java has method overloading so this may actually call one of several methods
 * depending on the number and type of arguments. This should be cached so that
 * only one instance is used for a given method name and class.
 * 
 * @author quark
 */
public class MethodHandle extends InvokableHandle<Method> {
	public MethodHandle(final String name, final Method[] methods) {
		super(name, methods);
	}

	@Override
	public Class[] getParameterTypes(final Method m) {
		return m.getParameterTypes();
	}

	@Override
	protected int getModifiers(final Method m) {
		return m.getModifiers();
	}
}
