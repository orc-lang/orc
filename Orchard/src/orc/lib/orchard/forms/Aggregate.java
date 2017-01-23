//
// Aggregate.java -- Java class Aggregate
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Aggregate implements Part<Map<String, Object>> {
    protected String key;
    protected List<Part<?>> parts = new LinkedList<Part<?>>();

    public Aggregate(final String key) {
        this.key = key;
    }

    @Override
    public Map<String, Object> getValue() {
        final HashMap<String, Object> out = new HashMap<String, Object>();
        for (final Part<?> part : parts) {
            out.put(part.getKey(), part.getValue());
        }
        return out;
    }

    public void addPart(final Part<?> part) {
        parts.add(part);
    }

    @Override
    public void readRequest(final FormData request, final List<String> errors) {
        for (final Part<?> part : parts) {
            part.readRequest(request, errors);
        }
    }

    @Override
    public void render(final PrintWriter out, final Set<String> flags) throws IOException {
        for (final Part<?> part : parts) {
            part.render(out, flags);
        }
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public boolean needsMultipartEncoding() {
        for (final Part<?> part : parts) {
            if (part.needsMultipartEncoding()) {
                return true;
            }
        }
        return false;
    }
}
