//
// FormInstructions.java -- Java class FormInstructions
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

public class FormInstructions implements Part<String> {
	private final String key;
	private final String value;

	public FormInstructions(final String key, final String value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public boolean needsMultipartEncoding() {
		return false;
	}

	@Override
	public void readRequest(final FormData request, final List<String> errors) {
		// do nothing
	}

	@Override
	public void render(final PrintWriter out, final Set<String> flags) throws IOException {
		out.write(value);
	}
}
