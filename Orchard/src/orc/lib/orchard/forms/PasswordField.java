//
// PasswordField.java -- Java class PasswordField
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

public class PasswordField extends Textbox {
    public PasswordField(final String key, final String label) {
        super(key, label);
    }

    @Override
    public void renderControl(final PrintWriter out) throws IOException {
        out.write("<input type='password'" + " id='" + key + "'" + " name='" + key + "'" + " value='" + XMLUtils.escapeXML(posted) + "'" + ">");
    }
}
