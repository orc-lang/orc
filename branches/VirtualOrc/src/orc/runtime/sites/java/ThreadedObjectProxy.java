//
// ThreadedObjectProxy.java -- Java class ThreadedObjectProxy
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

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.lib.util.ThreadSite;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;

/**
 * Objects whose methods should always be called in new threads. This is to be
 * avoided if possible, but it's safer than allowing native methods to block the
 * interpreter. The main reason you might need this is when wrapping some other
 * proxy, like a webservices proxy.
 * 
 * @author quark
 */
public class ThreadedObjectProxy extends Site {
	private final Object instance;
	private final ClassProxy classProxy;

	public ThreadedObjectProxy(final Object instance) {
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
			ThreadSite.makeThreaded(new MethodProxy(instance, classProxy.getMethod(caller, "apply"))).callSite(args, caller);
			return;
		}
		try {
			// try and return a method handle
			caller.resume(ThreadSite.makeThreaded(new MethodProxy(instance, classProxy.getMethod(caller, member))));
		} catch (final MessageNotUnderstoodException e) {
			try {
				// if a method was not found, return a field value
				caller.resume(classProxy.getField(caller, member).get(instance));
			} catch (final IllegalAccessException _) {
				throw e;
			} catch (final NoSuchFieldException _) {
				throw e;
			}
		}
	}
}
