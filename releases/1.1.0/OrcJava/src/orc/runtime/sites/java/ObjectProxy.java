//
// ObjectProxy.java -- Java class ObjectProxy
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

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Callable;

/**
 * A Java object being used as an Orc Site. This allows you to get a reference
 * to methods on the object using dot notation (like a DotSite).
 * 
 * <p>Methods are assumed to be non-blocking (although they may use {@link kilim}
 * for cooperative threading if desired). For objects with blocking methods,
 * use ThreadedObjectProxy.
 * 
 * @author dkitchin
 */
public class ObjectProxy extends Site {
	private final ClassProxy classProxy;
	private final Object instance;

	public static Callable proxyFor(final Object instance) {
		// we could use a hash map here to reuse proxies but
		// first we should find out if that's actually worthwhile
		if (instance.getClass().isArray()) {
			return new ObjectProxy(new ArrayProxy(instance));
		} else {
			return new ObjectProxy(instance);
		}
	}

	private ObjectProxy(final Object instance) {
		this.instance = instance;
		this.classProxy = ClassProxy.forClass(instance.getClass());
	}

	@Override
	public void callSite(final Args args, final Token caller) throws TokenException {
		String member;
		try {
			member = args.fieldName();
		} catch (final TokenException e) {
			// If this looks like a site call, call the special method "apply".
			new MethodProxy(instance, classProxy.getMethod(caller, "apply")).callSite(args, caller);
			return;
		}
		caller.resume(classProxy.getMember(caller, instance, member));
	}

	public Object getProxiedObject() {
		return instance;
	}
}
