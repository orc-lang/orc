//
// Textarea.java -- Java class Textarea
// Project Orchard
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

import orc.lib.net.XMLUtils;

public class Textarea extends SingleField<String> {
    private final boolean wrap;

    public Textarea(final String key, final String label, final String value, final boolean wrap) {
        super(key, label, value);
        this.wrap = wrap;
    }

    public Textarea(final String key, final String label, final String value) {
        this(key, label, value, true);
    }

    @Override
    public String requestToValue(final String posted) throws ValidationException {
        if (posted.trim().equals("")) {
            return null;
        }
        return posted.trim();
    }

    @Override
    public void renderControl(final PrintWriter out) throws IOException {
        out.write("<textarea" + " id='" + key + "'" + " name='" + key + "'" + " rows='6' cols='60'" + (wrap ? "" : " wrap='off'") + ">" + XMLUtils.escapeXML(posted) + "</textarea>");
    }
}
