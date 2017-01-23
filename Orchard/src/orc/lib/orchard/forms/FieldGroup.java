//
// FieldGroup.java -- Java class FieldGroup
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.orchard.forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public class FieldGroup extends Aggregate {
    private final String label;

    public FieldGroup(final String key, final String label) {
        super(key);
        this.label = label;
    }

    @Override
    public void render(final PrintWriter out, final Set<String> flags) throws IOException {
        out.write("<fieldset><legend>" + label + "</legend>");
        super.render(out, flags);
        out.write("</fieldset>");
    }
}
