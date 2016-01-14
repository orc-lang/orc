//
// UploadField.java -- Java class UploadField
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
import java.util.List;

import org.apache.commons.fileupload.FileItem;

public class UploadField extends Field<FileItem> {
    public UploadField(final String key, final String label) {
        super(key, label, null);
    }

    @Override
    public void renderControl(final PrintWriter out) throws IOException {
        out.write("<input type='file' name='" + key + "'>");
    }

    @Override
    public boolean needsMultipartEncoding() {
        return true;
    }

    @Override
    public void readRequest(final FormData request, final List errors) {
        value = request.getItem(key);
    }
}
