//
// Button.java -- Java class Button
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
import java.util.List;
import java.util.Set;

public class Button implements Part<Boolean> {
	private final String key;
	private final String label;
	private boolean clicked = false;

	public Button(final String key, final String label) {
		this.key = key;
		this.label = label;
	}

	@Override
	public void render(final PrintWriter out, final Set<String> flags) throws IOException {
		out.write("<input type='submit'" + " name='" + key + "'" + " value='" + label + "'" + ">");
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Boolean getValue() {
		return clicked;
	}

	@Override
	public boolean needsMultipartEncoding() {
		return false;
	}

	@Override
	public void readRequest(final FormData request, final List<String> errors) {
		clicked = request.getParameter(key) != null;
	}
}
