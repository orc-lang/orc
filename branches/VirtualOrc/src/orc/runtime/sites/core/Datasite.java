//
// Datasite.java -- Java class Datasite
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

package orc.runtime.sites.core;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.PartialSite;
import orc.runtime.values.TaggedValue;

public final class Datasite extends DotSite {

	public String tagName;

	public Datasite(final String tagname) {
		this.tagName = tagname;
	}

	@Override
	protected void addMembers() {
		addMember("?", new PartialSite() {
			@Override
			public Object evaluate(final Args args) throws TokenException {
				return deconstruct(args.getArg(0));
			}
		});
	}

	@Override
	protected void defaultTo(final Args args, final Token token) throws TokenException {
		token.resume(new TaggedValue(this, args.asArray()));
	}

	public Object deconstruct(final Object arg) throws TokenException {
		if (arg instanceof TaggedValue) {
			final TaggedValue v = (TaggedValue) arg;
			if (v.tag == this) {
				return Let.condense(v.values);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return tagName;
	}
}
