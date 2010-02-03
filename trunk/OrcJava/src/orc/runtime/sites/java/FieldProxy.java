//
// FieldProxy.java -- Java class FieldProxy
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

import java.lang.reflect.Field;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;

public class FieldProxy extends DotSite {
	private final Object instance;
	private final Field field;

	public FieldProxy(final Object instance, final Field field) {
		this.instance = instance;
		this.field = field;
	}

	@Override
	protected void addMembers() {
		addMember("read", new EvalSite() {
			@Override
			public Object evaluate(final Args args) throws TokenException {
				try {
					return field.get(instance);
				} catch (final IllegalArgumentException e) {
					throw new JavaException(e);
				} catch (final IllegalAccessException e) {
					throw new JavaException(e);
				}
			}
		});
		addMember("write", new EvalSite() {
			@Override
			public Object evaluate(final Args args) throws TokenException {
				try {
					field.set(instance, args.getArg(0));
					return signal();
				} catch (final IllegalArgumentException e) {
					throw new JavaException(e);
				} catch (final IllegalAccessException e) {
					throw new JavaException(e);
				}
			}
		});
	}
}
