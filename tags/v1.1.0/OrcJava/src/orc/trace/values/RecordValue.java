//
// RecordValue.java -- Java class RecordValue
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2008 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.trace.values;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import orc.trace.RecordTerm;
import orc.trace.Term;
import orc.trace.Terms;

/**
 * This doesn't correspond to any specific Orc
 * type but is instead intended to be used by
 * implementors of {@link TraceableValue} to
 * encode object-like immutable structures.
 */
public class RecordValue extends AbstractValue implements RecordTerm {
	Map<String, Value> properties = new HashMap<String, Value>();
	public final Class class_;

	/**
	 * Ok, so this isn't <i>really</i> immutable; you
	 * have to call {@link #put(String, Value)} to actually
	 * set the mapped values. Just promise not to change
	 * anything after you serialize the object.
	 */
	public RecordValue(final Class class_) {
		this.class_ = class_;
	}

	public void put(final String key, final Value value) {
		properties.put(key, value);
	}

	public void prettyPrint(final Writer out, final int indent) throws IOException {
		out.write(class_.getName());
		Terms.prettyPrintMap(out, indent, properties.entrySet());
	}

	public Term getProperty(final String key) {
		return properties.get(key);
	}
}
