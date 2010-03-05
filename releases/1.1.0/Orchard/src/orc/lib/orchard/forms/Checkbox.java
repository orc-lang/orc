//
// Checkbox.java -- Java class Checkbox
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

public class Checkbox extends SingleField<Boolean> {

	public Checkbox(final String key, final String label) {
		super(key, label, null);
	}

	@Override
	public void renderControl(final PrintWriter out) throws IOException {
		out.write("<input type='checkbox'" + " id='" + key + "'" + " name='" + key + "'" + (posted == null ? "" : " checked") + ">");
	}

	@Override
	public Boolean requestToValue(final String posted) throws ValidationException {
		return posted != null;
	}
}
