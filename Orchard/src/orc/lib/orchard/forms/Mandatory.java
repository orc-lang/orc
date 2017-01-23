//
// Mandatory.java -- Java class Mandatory
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
import java.util.List;
import java.util.Set;

public class Mandatory<V> implements Part<V> {
    private final Field<V> part;

    public Mandatory(final Field<V> part) {
        this.part = part;
    }

    @Override
    public String getKey() {
        return part.getKey();
    }

    @Override
    public V getValue() {
        return part.getValue();
    }

    @Override
    public boolean needsMultipartEncoding() {
        return part.needsMultipartEncoding();
    }

    @Override
    public void readRequest(final FormData request, final List<String> errors) {
        part.readRequest(request, errors);
        if (getValue() == null) {
            errors.add("Please provide " + part.getLabel());
        }
    }

    @Override
    public void render(final PrintWriter out, final Set<String> flags) throws IOException {
        part.renderHeader(out, flags);
        out.write("<label for='" + part.getKey() + "'>" + part.getLabel());
        part.renderControl(out);
        out.write(" <i>(required)</i>");
        out.write("</label>");
    }
}
