//
// Textbox.java -- Java class Textbox
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

public class Textbox extends SingleField<String> {

    public Textbox(final String key, final String label) {
        super(key, label, "");
    }

    @Override
    public String requestToValue(final String posted) throws ValidationException {
        if (posted.equals("")) {
            return null;
        }
        return posted;
    }

    @Override
    public void renderControl(final PrintWriter out) throws IOException {
        out.write("<input type='textbox'" + " id='" + key + "'" + " name='" + key + "'" + " value='" + XMLUtils.escapeXML(posted) + "'" + ">");
    }
}
