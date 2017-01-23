//
// Part.java -- Java interface Part
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

public interface Part<V> {
    /**
     * Render this form part to the given response using its printwriter.
     * 
     * @throws IOException
     */
    public void render(PrintWriter out, Set<String> flags) throws IOException;

    /** Return the value of the part. */
    public V getValue();

    /**
     * Read a value from the request. Append any error messages to the given
     * list.
     */
    public void readRequest(FormData request, List<String> errors);

    /** A unique name for this part. */
    public String getKey();

    /** True if this part needs to use form/multipart encoding */
    public boolean needsMultipartEncoding();
}
