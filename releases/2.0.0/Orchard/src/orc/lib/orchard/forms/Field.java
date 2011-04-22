//
// Field.java -- Java class Field
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public abstract class Field<V> implements Part<V> {

	protected String label;
	protected String key;
	protected V value;

	public Field(final String key, final String label, final V value) {
		this.key = key;
		this.label = label;
		this.value = value;
	}

	@Override
	public String getKey() {
		return key;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public V getValue() {
		return value;
	}

	@Override
	public void render(final PrintWriter out, final Set<String> flags) throws IOException {
		renderHeader(out, flags);
		out.write("<label for='" + key + "'>" + label);
		renderControl(out);
		out.write("</label>");
	}

	@Override
	public boolean needsMultipartEncoding() {
		return false;
	}

	public void renderHeader(final PrintWriter out, final Set<String> flags) throws IOException {
		// do nothing
	}

	public abstract void renderControl(PrintWriter out) throws IOException;
}
