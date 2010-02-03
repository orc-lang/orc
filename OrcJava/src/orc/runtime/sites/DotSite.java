//
// DotSite.java -- Java class DotSite
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

package orc.runtime.sites;

import java.util.Map;
import java.util.TreeMap;

import orc.error.runtime.MessageNotUnderstoodException;
import orc.error.runtime.TokenException;
import orc.error.runtime.UncallableValueException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.trace.values.Marshaller;
import orc.trace.values.RecordValue;
import orc.trace.values.TraceableValue;
import orc.trace.values.Value;

/**
 * Dot-accessible sites should extend this class and declare their Orc-available
 * methods using {@link #addMembers()}. The code is forward-compatible with many possible
 * optimizations on the field lookup strategy.
 * 
 * A dot site may also have a default behavior which allows it to behave like
 * a normal site. If its argument is not a message, it displays that 
 * default behavior, if implemented. If there is no default behavior, it
 * raises a type error.
 * 
 * @author dkitchin
 */
public abstract class DotSite extends Site implements TraceableValue {

	Map<String, Object> methodMap;

	public DotSite() {
		methodMap = new TreeMap<String, Object>();
		this.addMembers();
	}

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public void callSite(final Args args, final Token t) throws TokenException {

		String f;

		// Check if the argument is a message
		try {
			f = args.fieldName();
		} catch (final TokenException e) {
			// If not, invoke the default behavior and return.
			defaultTo(args, t);
			return;
		}

		// If it is a message, look it up.
		final Object m = getMember(f);
		if (m != null) {
			t.resume(m);
		} else {
			throw new MessageNotUnderstoodException(f);
		}
	}

	Object getMember(final String f) {
		return methodMap.get(f);
	}

	protected abstract void addMembers();

	protected void addMember(final String f, final Object s) {
		methodMap.put(f, s);
	}

	protected void defaultTo(final Args args, final Token token) throws TokenException {
		throw new UncallableValueException("This dot site has no default behavior; it only responds to messages.");
	}

	public Value marshal(final Marshaller tracer) {
		final RecordValue out = new RecordValue(getClass());
		for (final Map.Entry<String, Object> entry : methodMap.entrySet()) {
			out.put(entry.getKey(), tracer.marshal(entry.getValue()));
		}
		return out;
	}
}
