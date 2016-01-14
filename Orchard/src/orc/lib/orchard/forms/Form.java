//
// Form.java -- Java class Form
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import orc.lib.net.XMLUtils;

public class Form extends Aggregate {
    private final Map<String, String> hiddens = new HashMap<String, String>();

    public Form() {
        super("");
    }

    public void setHidden(final String key, final String value) {
        hiddens.put(key, value);
    }

    @Override
    public void render(final PrintWriter out, final Set<String> flags) throws IOException {
        out.write("<form method='post'");
        if (needsMultipartEncoding()) {
            out.write(" enctype='multipart/form-data'");
        }
        out.write(">");
        for (final Map.Entry<String, String> hidden : hiddens.entrySet()) {
            out.write("<input type='hidden'" + " name='" + hidden.getKey() + "'" + " value='" + XMLUtils.escapeXML(hidden.getValue()) + "'" + ">");
        }
        super.render(out, flags);
        out.write("</form>");
    }
}
